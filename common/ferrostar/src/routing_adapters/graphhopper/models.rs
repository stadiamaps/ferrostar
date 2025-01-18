use serde::{Deserialize};
use alloc::collections::BTreeMap as HashMap;
use crate::models::{ GeographicCoordinate };

#[derive(Deserialize, Debug)]
pub struct GraphHopperRouteResponse {
    pub paths: Vec<GraphHopperPath>,
    pub message: Option<String>
}

#[derive(Deserialize, Debug)]
#[serde(untagged)]
pub enum DetailEntryValue {
    Int(i32),
    Str(String),
    Bool(bool),
    Float(f64),
}

#[derive(Deserialize, Debug)]
#[serde(from = "Vec<serde_json::Value>")]
pub struct DetailEntry {
    pub start_index: usize,
    pub end_index: usize,
    pub value: Option<DetailEntryValue>,
}

impl From<Vec<serde_json::Value>> for DetailEntry {
    fn from(vec: Vec<serde_json::Value>) -> Self {
        let start_index = vec[0].as_i64().unwrap_or_default() as usize;
        let end_index = vec[1].as_i64().unwrap_or_default() as usize;

        let value = match &vec[2] {
            serde_json::Value::String(s) => Some(DetailEntryValue::Str(s.clone())),
            serde_json::Value::Number(n) => {
                if n.is_i64() {
                    Some(DetailEntryValue::Int(n.as_i64().unwrap() as i32))
                } else {
                    Some(DetailEntryValue::Float(n.as_f64().unwrap()))
                }
            },
            serde_json::Value::Bool(b) => Some(DetailEntryValue::Bool(*b)),
            serde_json::Value::Null => None,
            _ => None,
        };

        DetailEntry { start_index, end_index, value }
    }
}

#[derive(Deserialize, Debug)]
pub struct GraphHopperPath {
    pub distance: f64,
    pub time: f64,
    pub bbox: Vec<f64>,
    pub instructions: Vec<GraphHopperInstruction>,
    pub details: HashMap<String, Vec<DetailEntry>>,
    pub points: String, // encoded polyline
    pub points_encoded:	bool,
    pub points_encoded_multiplier: f64,
}

#[derive(Deserialize, Debug)]
pub struct GraphHopperInstruction {
    pub distance: f64,
    pub time: f64,
    pub heading: Option<f64>,
    pub exit_number: Option<u32>,
    pub turn_angle: Option<f64>,
    pub sign: i32,
    pub interval: Vec<usize>,
    pub text: String,
    pub street_ref: Option<String>,
    pub street_name: String,
}

// temporary structure until we understand internal maxspeed handling
pub struct MaxSpeedEntry {
    pub geometry: Vec<GeographicCoordinate>,
    pub speed_limit: Option<f64>,
    pub unit: String,
}
