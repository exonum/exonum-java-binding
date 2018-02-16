use exonum::storage::MapIndex;
use exonum::storage::{Fork, Snapshot};

const SHADOW_TABLE_NAME: &str = "__INDEXES_INFO__";

encoding_struct!(
    struct IndexInfo {
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
        use self::TableType::*;
        match tt {
            Entry => 0,
            KeySet => 1,
            List => 2,
            Map => 3,
            ProofList => 4,
            ProofMap => 5,
            ValueSet => 6,
        }
    }
}

impl From<u8> for TableType {
    fn from(num: u8) -> Self {
        use self::TableType::*;
        match num {
            0 => Entry,
            1 => KeySet,
            2 => List,
            3 => Map,
            4 => ProofList,
            5 => ProofMap,
            6 => ValueSet,
            invalid => {
                panic!(
                    "Unreachable pattern ({:?}) while constructing table type. Storage data is probably corrupted",
                    invalid
                )
            }
        }
    }
}

fn shadow_table<T>(view: T) -> MapIndex<T, String, IndexInfo> {
    MapIndex::new(SHADOW_TABLE_NAME, view)
}

pub fn check_read<T: AsRef<Snapshot>>(name: &str, table_type: TableType, view: T) {
    let shadow_table = shadow_table(view);
    if let Some(value) = shadow_table.get(&name.to_owned()) {
        let stored_type = TableType::from(value.table_type());
        assert_eq!(
            stored_type,
            table_type,
            "Attempt to access index of type {:?}, while said index was initially created with type {:?}",
            table_type,
            stored_type
        );
    }
}

pub fn check_write(name: &str, table_type: TableType, view: &mut Fork) {
    if name == SHADOW_TABLE_NAME {
        panic!("Attempt to access an internal storage infrastructure");
    }
    let mut shadow_table = shadow_table(view);
    if let Some(value) = shadow_table.get(&name.to_owned()) {
        let stored_type = TableType::from(value.table_type());
        assert_eq!(
            stored_type,
            table_type,
            "Attempt to access index of type {:?}, while said index was initially created with type {:?}",
            table_type,
            stored_type
        );
    } else {
        shadow_table.put(&name.to_owned(), IndexInfo::new(table_type.into()));
    }
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
