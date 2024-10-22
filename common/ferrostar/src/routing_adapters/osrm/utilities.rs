use super::models::AnyAnnotation;
use crate::models::AnyAnnotationValue;
use crate::models::CongestionSegment;
use crate::models::GeographicCoordinate;
use crate::routing_adapters::error::ParsingError;
use serde_json::Value;
use std::collections::HashMap;

/// Gets a slice of the route's annotations array.
///
/// Returns a [`ParsingError`] if the annotations are not present or the slice is out of bounds.
pub(crate) fn get_annotation_slice(
    annotations: Option<Vec<AnyAnnotationValue>>,
    start_index: usize,
    end_index: usize,
) -> Result<Vec<AnyAnnotationValue>, ParsingError> {
    annotations
        .ok_or(ParsingError::MalformedAnnotations {
            error: "No annotations".to_string(),
        })?
        .get(start_index..end_index)
        .ok_or(ParsingError::MalformedAnnotations {
            error: "Annotations slice index out of bounds ({start_index}..{end_index})".to_string(),
        })
        .map(<[AnyAnnotationValue]>::to_vec)
}

/// Converts the the OSRM-style annotation object consisting of separate arrays
/// to a single vector of parsed objects (one for each coordinate pair).
pub(crate) fn zip_annotations(annotation: AnyAnnotation) -> Vec<AnyAnnotationValue> {
    let source: HashMap<String, Vec<Value>> = annotation.values;

    // Get the length of the array (assumed to be the same for all annotations)
    let length = source.values().next().map_or(0, Vec::len);

    return (0..length)
        .map(|i| {
            source
                .iter()
                .filter_map(|(key, values)| {
                    // Values is the vector at a specific key in the original annotations object.
                    values
                        .get(i) // For each key, get the value at the index i.
                        .map(|v| (key.clone(), v.clone())) // Convert the key and value to a tuple.
                })
                .collect::<HashMap<String, Value>>() // Collect the key-value pairs into a hashmap.
        })
        .map(|value| AnyAnnotationValue { value })
        .collect::<Vec<AnyAnnotationValue>>();
}

pub(crate) fn extract_congestion_segments(
    annotations: Vec<AnyAnnotation>,
    geometry: &[GeographicCoordinate],
) -> Result<Vec<CongestionSegment>, ParsingError> {
    let mut segments = Vec::new();
    let mut overall_index = 0;

    for annotation in annotations.iter() {
        let congestion = match annotation.values.get("congestion") {
            Some(values) => values
                .iter()
                .map(|v| v.as_str().map(|s| s.to_string()))
                .collect::<Vec<_>>(),
            None => vec![],
        };

        let distances = match annotation.values.get("distance") {
            Some(values) => values.iter().map(|v| v.as_f64()).collect::<Vec<_>>(),
            None => vec![],
        };

        let mut current_index = overall_index;

        for (level, distance) in congestion.iter().zip(distances.iter()) {
            if let (Some(level), Some(distance)) = (level, distance) {
                let start_index = current_index;
                let mut end_index = current_index;
                let mut remaining_distance = *distance;

                while end_index < geometry.len() - 1 && remaining_distance > 0.0 {
                    let segment_distance =
                        haversine_distance(&geometry[end_index], &geometry[end_index + 1]);
                    if remaining_distance >= segment_distance {
                        remaining_distance -= segment_distance;
                        end_index += 1;
                    } else {
                        break;
                    }
                }

                if end_index >= geometry.len() {
                    break;
                }

                let segment_geometry: Vec<GeographicCoordinate> = geometry
                    .iter()
                    .skip(start_index)
                    .take(end_index - start_index + 1)
                    .cloned()
                    .collect();

                segments.push(CongestionSegment {
                    level: level.clone(),
                    geometry: segment_geometry,
                });

                current_index = end_index;
            }
            // if either level or distance is None we skip this segment
        }
        overall_index = current_index;
    }

    Ok(segments)
}

fn haversine_distance(coord1: &GeographicCoordinate, coord2: &GeographicCoordinate) -> f64 {
    const EARTH_RADIUS: f64 = 6371000.0;

    let lat1 = coord1.lat.to_radians();
    let lat2 = coord2.lat.to_radians();
    let delta_lat = (coord2.lat - coord1.lat).to_radians();
    let delta_lon = (coord2.lng - coord1.lng).to_radians();

    let a =
        (delta_lat / 2.0).sin().powi(2) + lat1.cos() * lat2.cos() * (delta_lon / 2.0).sin().powi(2);
    let c = 2.0 * a.sqrt().atan2((1.0 - a).sqrt());

    EARTH_RADIUS * c
}

#[cfg(test)]
mod test {

    use super::*;
    use serde_json::json;
    use serde_json::Map;

    #[test]
    fn test_zip_annotation() {
        let json_str = r#"{
            "distance": [1.2, 2.24, 3.24],
            "duration": [4, 5, 6],
            "speed": [10, 11, 12],
            "max_speed": [{
              "speed": 56,
              "unit": "km/h"
            }, {
              "speed": 12,
              "unit": "mi/h"
            }, {
              "unknown": true
            }],
            "traffic": ["bad", "ok", "good"],
            "construction": [null, true, null]
        }"#;

        let json_value: Map<String, Value> = serde_json::from_str(json_str).unwrap();
        let values: HashMap<String, Vec<Value>> = json_value
            .iter()
            .map(|(k, v)| (k.to_string(), v.as_array().unwrap().clone()))
            .collect();

        // Construct the annotation object.
        let annotation = AnyAnnotation { values };
        let result = zip_annotations(annotation);

        insta::with_settings!({sort_maps => true}, {
            insta::assert_yaml_snapshot!(result);
        });
    }

    fn create_annotation(congestion: Vec<&str>, distances: Vec<f64>) -> AnyAnnotation {
        let mut values = HashMap::new();
        values.insert(
            "congestion".to_string(),
            congestion.into_iter().map(|s| json!(s)).collect(),
        );
        values.insert(
            "distance".to_string(),
            distances.into_iter().map(|d| json!(d)).collect(),
        );
        AnyAnnotation { values }
    }

    #[test]
    fn test_extract_congestion_segments_basic() {
        let annotations = vec![create_annotation(
            vec!["low", "moderate", "heavy"],
            vec![10.0, 20.0, 30.0],
        )];
        let geometry = vec![
            GeographicCoordinate { lat: 0.0, lng: 0.0 },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.001,
            },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.002,
            },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.003,
            },
        ];

        let result = extract_congestion_segments(annotations, &geometry).unwrap();

        assert_eq!(result.len(), 3);
        assert_eq!(result[0].level, "low");
        assert_eq!(result[1].level, "moderate");
        assert_eq!(result[2].level, "heavy");
    }

    #[test]
    fn test_extract_congestion_segments_empty() {
        let annotations = vec![];
        let geometry = vec![];

        let result = extract_congestion_segments(annotations, &geometry).unwrap();

        assert!(result.is_empty());
    }

    #[test]
    fn test_extract_congestion_segments_missing_data() {
        let mut annotation = AnyAnnotation {
            values: HashMap::new(),
        };
        annotation
            .values
            .insert("congestion".to_string(), vec![json!("low")]);
        // Missing distance data
        let annotations = vec![annotation];
        let geometry = vec![
            GeographicCoordinate { lat: 0.0, lng: 0.0 },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.001,
            },
        ];

        let result = extract_congestion_segments(annotations, &geometry).unwrap();

        assert!(result.is_empty());
    }

    #[test]
    fn test_extract_congestion_segments_multiple_annotations() {
        let annotations = vec![
            create_annotation(vec!["low", "moderate"], vec![10.0, 20.0]),
            create_annotation(vec!["heavy", "severe"], vec![30.0, 40.0]),
        ];
        let geometry = vec![
            GeographicCoordinate { lat: 0.0, lng: 0.0 },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.001,
            },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.002,
            },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.003,
            },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.004,
            },
        ];

        let result = extract_congestion_segments(annotations, &geometry).unwrap();

        assert_eq!(result.len(), 4);
        assert_eq!(result[0].level, "low");
        assert_eq!(result[1].level, "moderate");
        assert_eq!(result[2].level, "heavy");
        assert_eq!(result[3].level, "severe");
    }

    #[test]
    fn test_extract_congestion_segments_large_distances() {
        let annotations = vec![create_annotation(
            vec!["low", "moderate"],
            vec![1000000.0, 2000000.0],
        )];
        let geometry = vec![
            GeographicCoordinate { lat: 0.0, lng: 0.0 },
            GeographicCoordinate { lat: 1.0, lng: 1.0 },
            GeographicCoordinate { lat: 2.0, lng: 2.0 },
        ];

        let result = extract_congestion_segments(annotations, &geometry).unwrap();

        assert_eq!(result.len(), 2);
        assert_eq!(result[0].level, "low");
        assert_eq!(result[1].level, "moderate");
        assert!(result[0].geometry.len() >= 2);
        assert!(result[1].geometry.len() >= 1);
    }

    #[test]
    fn test_extract_congestion_segments_null_values() {
        let mut annotation = AnyAnnotation {
            values: HashMap::new(),
        };
        annotation.values.insert(
            "congestion".to_string(),
            vec![json!("low"), json!(null), json!("high")],
        );
        annotation.values.insert(
            "distance".to_string(),
            vec![json!(10.0), json!(20.0), json!(30.0)],
        );
        let annotations = vec![annotation];
        let geometry = vec![
            GeographicCoordinate { lat: 0.0, lng: 0.0 },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.001,
            },
            GeographicCoordinate {
                lat: 0.0,
                lng: 0.002,
            },
        ];

        let result = extract_congestion_segments(annotations, &geometry).unwrap();

        assert_eq!(result.len(), 2);
        assert_eq!(result[0].level, "low");
        assert_eq!(result[1].level, "high");
    }
}
