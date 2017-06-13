use std::mem;

use exonum::storage2::{Snapshot, Fork, StorageKey};

// TODO: Replace by simple typedef when `StorageKey` is implemented for `Vec<u8>`.
pub struct Key(pub Vec<u8>);
pub type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub enum View {
    Snapshot(Box<Snapshot>),
    Fork(Fork),
}

impl StorageKey for Key {
    fn size() -> usize {
        mem::size_of::<Vec<u8>>()
    }

    fn write(&self, buffer: &mut Vec<u8>) {
        buffer.copy_from_slice(&self.0)
    }

    fn from_slice(buffer: &[u8]) -> Self {
        Key(buffer.to_vec())
    }
}