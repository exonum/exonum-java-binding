use exonum::storage::{Snapshot, Fork};

// TODO: Temporary solution, should be replaced by the same typedef as `Value`.
pub type Key = u8;
pub type Value = Vec<u8>;

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub enum View {
    Snapshot(Box<Snapshot>),
    Fork(Fork),
}
