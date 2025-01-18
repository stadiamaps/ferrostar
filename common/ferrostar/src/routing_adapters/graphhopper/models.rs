use serde::{Deserialize};


#[derive(Deserialize, Debug)]
pub struct GraphHopperRouteResponse {
    pub paths: Vec<GraphHopperPath>,
    pub message: Option<String>
}

/*
// path details currently unused
use alloc::collections::BTreeMap as HashMap;

#[derive(Deserialize, Debug)]
#[serde(untagged)]
pub enum DetailEntryValue {
    Int(i32),
    Str(String),
    Bool(bool),
    Float(f64),
    Null,
}

#[derive(Deserialize, Debug)]
#[serde(from = "Vec<serde_json::Value>")]
pub struct DetailEntry {
    first: u32, // probably usize better like we do for instruction.interval
    second: u32,
    value: DetailEntryValue,
}

impl From<Vec<serde_json::Value>> for DetailEntry {
    fn from(vec: Vec<serde_json::Value>) -> Self {
        let first = vec[0].as_i64().unwrap_or_default() as u32;
        let second = vec[1].as_i64().unwrap_or_default() as u32;

        let value = match &vec[2] {
            serde_json::Value::String(s) => DetailEntryValue::Str(s.clone()),
            serde_json::Value::Number(n) => {
                if n.is_i64() {
                    DetailEntryValue::Int(n.as_i64().unwrap() as i32)
                } else {
                    DetailEntryValue::Float(n.as_f64().unwrap())
                }
            },
            serde_json::Value::Bool(b) => DetailEntryValue::Bool(*b),
            serde_json::Value::Null => DetailEntryValue::Null,
            _ => DetailEntryValue::Null,
        };

        DetailEntry { first, second, value }
    }
}
*/

#[derive(Deserialize, Debug)]
pub struct GraphHopperPath {
    pub distance: f64,
    pub time: f64,
    pub bbox: Vec<f64>,
    pub instructions: Vec<GraphHopperInstruction>,
    // pub details: HashMap<String, Vec<DetailEntry>>,
    pub points: String, // encoded polyline
    pub points_encoded:	bool,
    pub points_encoded_multiplier: f64,
}

#[derive(Deserialize, Debug)]
pub struct GraphHopperInstruction {
    pub distance: f64,
    pub time: f64,
    pub heading: Option<f64>,
    pub turn_angle: Option<f64>,
    pub sign: i32,
    pub interval: Vec<usize>,
    pub text: String,
    pub street_ref: Option<String>,
    pub street_name: String,
}
