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

fn indexes_metadata<T>(view: T) -> MapIndex<T, String, IndexInfo> {
    MapIndex::new(SHADOW_TABLE_NAME, view)
}

fn assert_table_type(name: &str, table_type: TableType, stored_type: TableType) {
    assert_eq!(
        stored_type,
        table_type,
        "Attempt to access index '{}' of type {:?}, while said index was initially created with type {:?}",
        name,
        table_type,
        stored_type
    );
}

pub fn check_read<T: AsRef<Snapshot>>(name: &str, table_type: TableType, view: T) {
    let metadata = indexes_metadata(view);
    if let Some(value) = metadata.get(&name.to_owned()) {
        let stored_type = TableType::from(value.table_type());
        assert_table_type(name, table_type, stored_type);
    }
}

pub fn check_write(name: &str, table_type: TableType, view: &mut Fork) {
    if name == SHADOW_TABLE_NAME {
        panic!("Attempt to access an internal storage infrastructure");
    }
    let mut metadata = indexes_metadata(view);
    if let Some(value) = metadata.get(&name.to_owned()) {
        let stored_type = TableType::from(value.table_type());
        assert_table_type(name, table_type, stored_type);
    } else {
        metadata.put(&name.to_owned(), IndexInfo::new(table_type.into()));
    }
}

#[cfg(test)]
mod tests {
    use std::panic;
    use std::ops::DerefMut;

    use super::{TableType, check_read, check_write, SHADOW_TABLE_NAME};
    use exonum::storage::{MemoryDB, Database};

    #[test]
    fn table_type_roundtrip() {
        for i in 0..7 {
            let table_type: TableType = i.into();
            let num: u8 = table_type.into();
            assert_eq!(i, num);
        }
    }

    #[test]
    fn access_indexes_metadata() {
        let database = MemoryDB::new();
        let mut fork = database.fork();

        check_read(SHADOW_TABLE_NAME, TableType::Map, &mut fork);

        let result = {
            let mut fork = panic::AssertUnwindSafe(&mut fork);
            panic::catch_unwind(move || {
                check_write(SHADOW_TABLE_NAME, TableType::Map, fork.deref_mut());
            })
        };

        assert!(result.is_err());
    }

    #[test]
    fn invalid_table_type() {
        let database = MemoryDB::new();
        let mut fork = database.fork();

        check_write("test_index", TableType::ProofMap, &mut fork);

        let result = {
            let mut fork = panic::AssertUnwindSafe(&mut fork);
            panic::catch_unwind(move || {
                check_read("test_index", TableType::Map, fork.deref_mut());
            })
        };

        assert!(result.is_err());
    }

    #[test]
    fn valid_table_type() {
        let database = MemoryDB::new();
        let mut fork = database.fork();

        check_write("test_index", TableType::ProofMap, &mut fork);
        check_read("test_index", TableType::ProofMap, &mut fork);
    }

    #[test]
    fn multiple_read_before_write() {
        let database = MemoryDB::new();
        let mut fork = database.fork();

        check_read("test_index", TableType::Map, &mut fork);
        check_read("test_index", TableType::List, &mut fork);
        check_read("test_index", TableType::Entry, &mut fork);
        check_read("test_index", TableType::KeySet, &mut fork);
        check_read("test_index", TableType::ValueSet, &mut fork);
        check_read("test_index", TableType::ProofMap, &mut fork);
        check_read("test_index", TableType::ProofList, &mut fork);

        // Lock the type
        check_write("test_index", TableType::ProofMap, &mut fork);
        check_read("test_index", TableType::ProofMap, &mut fork);

        // Make sure we're unable to read with different type now
        let result = {
            let mut fork = panic::AssertUnwindSafe(&mut fork);
            panic::catch_unwind(move || {
                check_write(SHADOW_TABLE_NAME, TableType::Map, fork.deref_mut());
            })
        };

        assert!(result.is_err());
    }
}
