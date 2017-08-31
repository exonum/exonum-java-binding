use jni::JNIEnv;
use jni::objects::{GlobalRef, JMethodID};
use jni::errors::Result;

use std::mem;

pub struct PairIter<InnerIter> {
    pub iter: InnerIter,
    pub element_class: GlobalRef,
    pub constructor_id: JMethodID<'static>,
}

impl<InnerIter> PairIter<InnerIter> {
    pub fn new(env: &JNIEnv, iter: InnerIter, class_name: &str) -> Result<Self> {
        let class = env.find_class(class_name)?;
        let element_class = env.new_global_ref(class.into())?;
        let id = env.get_method_id(class_name, "<init>", "([B[B)V")?;
        Ok(PairIter {
            iter,
            element_class,
            constructor_id: unsafe { mem::transmute(id) },
        })
    }
}
