use super::models::{Annotation, AnnotationValue, MaxSpeed, MaxSpeedUnits};
use itertools::izip;

/// Get's he slice of annotations
pub(crate) fn get_annotation_slice(
    start_index: usize,
    end_index: usize,
    annotations: Option<Vec<AnnotationValue>>,
) -> Option<Vec<AnnotationValue>> {
    return if let Some(annotations) = &annotations {
        let annot_len = annotations.len();

        println!("Step Indexes: (start_index = {start_index}, end_index={end_index}, annotation.len: {annot_len})");

        let slice = &annotations[start_index..end_index];
        Some(slice.to_vec())
    } else {
        None
    };
}

/// Converts the the OSRM-style annotation object consisting of separate arrays
/// to a single vector of parsed objects (one for each coordinate pair).
pub(crate) fn zip_annotations(annotations: Annotation) -> Vec<AnnotationValue> {
    izip!(
        annotations.distance,
        annotations.duration,
        annotations.max_speed,
        annotations.speed
    )
    .map(|(distance, duration, max_speed, speed)| AnnotationValue {
        distance,
        duration,
        max_speed_mps: convert_max_speed_to_mps(max_speed),
        speed_mps: speed,
    })
    .collect()
}

/// Converts a max speed value to meters per second.
pub(crate) fn convert_max_speed_to_mps(max_speed: MaxSpeed) -> Option<f64> {
    match max_speed {
        MaxSpeed::Known { speed, unit } => match unit {
            MaxSpeedUnits::KilometersPerHour => return Some(speed * 0.27778),
            MaxSpeedUnits::MilesPerHour => return Some(speed * 0.44704),
        },
        #[allow(unused)]
        MaxSpeed::Unknown { unknown } => return None,
    }
}

// TODO: Snapshot test a zip annotations test case.

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn test_max_speed_unknown() {
        let max_speed = MaxSpeed::Unknown { unknown: true };
        assert_eq!(convert_max_speed_to_mps(max_speed), None);
    }

    #[test]
    fn test_max_speed_kph() {
        let max_speed = MaxSpeed::Known {
            speed: 100.0,
            unit: MaxSpeedUnits::KilometersPerHour,
        };
        assert_eq!(
            convert_max_speed_to_mps(max_speed),
            Some(27.778000000000002)
        );
    }

    #[test]
    fn test_max_speed_mph() {
        let max_speed = MaxSpeed::Known {
            speed: 60.0,
            unit: MaxSpeedUnits::MilesPerHour,
        };
        assert_eq!(convert_max_speed_to_mps(max_speed), Some(26.8224));
    }
}
