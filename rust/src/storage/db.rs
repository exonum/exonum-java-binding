use exonum::storage2::{Snapshot, Fork};

// Raw pointer to the `View` is returned to the java side, so in rust functions that take back
// `Snapshot` or`Fork` it will be possible to distinguish them.
pub enum View {
    Snapshot(Box<Snapshot>),
    Fork(Fork),
}
