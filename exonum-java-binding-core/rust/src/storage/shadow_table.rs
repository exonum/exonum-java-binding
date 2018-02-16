use exonum::storage::MapIndex;
use exonum::storage::{Fork, Snapshot};

type Key = String;

const SHADOW_TABLE_NAME: &str = "__SHADOW_TABLE__";

encoding_struct!(
    struct Value {
        table_type: u8,
    }
);

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TableType {
    Entry,
    KeySet,
    List,
    Map,
    ProofList,
    ProofMap,
    ValueSet,
}

impl From<TableType> for u8 {
    fn from(tt: TableType) -> Self {
        match tt {
            TableType::Entry => 0,
            TableType::KeySet => 1,
            TableType::List => 2,
            TableType::Map => 3,
            TableType::ProofList => 4,
            TableType::ProofMap => 5,
            TableType::ValueSet => 6,
        }
    }
}

impl From<u8> for TableType {
    fn from(num: u8) -> Self {
        match num {
            0 => TableType::Entry,
            1 => TableType::KeySet,
            2 => TableType::List,
            3 => TableType::Map,
            4 => TableType::ProofList,
            5 => TableType::ProofMap,
            6 => TableType::ValueSet,
            _ => {
                panic!(
                    "Unreachable pattern while constructing table type. Storage data is probably corrupted"
                )
            }
        }
    }
}

type Index<T> = MapIndex<T, Key, Value>;

pub fn try_read<T: AsRef<Snapshot>>(
    name: &str,
    table_type: TableType,
    view: T,
) -> Result<(), String> {
    let shadow_table: Index<T> = MapIndex::new(SHADOW_TABLE_NAME, view);
    if let Some(value) = shadow_table.get(&name.to_owned()) {
        let stored_type = TableType::from(value.table_type());
        if stored_type != table_type {
            return Err(format!(
                "Attempt to access index of type {:?}, while said index was initially created with type {:?}",
                table_type,
                stored_type,
            ));
        }
    }
    Ok(())
}


pub fn try_write(name: &str, table_type: TableType, view: &mut Fork) -> Result<(), String> {
    if name == SHADOW_TABLE_NAME {
        return Err(
            "Attempt to access an internal storage infrastructure".to_owned(),
        );
    }
    let mut shadow_table: Index<&mut Fork> = MapIndex::new(SHADOW_TABLE_NAME, view);
    if let Some(value) = shadow_table.get(&name.to_owned()) {
        let stored_type = TableType::from(value.table_type());
        if stored_type != table_type {
            return Err(format!(
                "Attempt to access index of type {:?}, while said index was initially created with type {:?}",
                table_type,
                stored_type,
            ));
        }
    } else {
        shadow_table.put(&name.to_owned(), Value::new(table_type.into()));
    }
    Ok(())
}


#[cfg(test)]
mod tests {
    use super::TableType;

    #[test]
    fn table_type_roundtrip() {
        for i in 0..7 {
            let table_type: TableType = i.into();
            let num: u8 = table_type.into();
            assert_eq!(i, num);
        }
    }
}
