use super::models::AnyAnnotation;
use crate::models::AnyAnnotationValue;
use crate::routing_adapters::error::ParsingError;
use serde_json::Value;
use std::collections::HashMap;

/// Get's the slice of annotations
///
/// Throws an [`ParsingError`] if the annotations are not present or the slice is out of bounds.
pub(crate) fn get_annotation_slice(
    annotations: Option<Vec<AnyAnnotationValue>>,
    start_index: usize,
    end_index: usize,
) -> Result<Vec<AnyAnnotationValue>, ParsingError> {
    annotations
        .ok_or(ParsingError::Annotations {
            error: "No annotations".to_string(),
        })?
        .get(start_index..end_index)
        .ok_or(ParsingError::Annotations {
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

#[cfg(test)]
mod test {

    use serde_json::Map;

    use super::*;

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
}
