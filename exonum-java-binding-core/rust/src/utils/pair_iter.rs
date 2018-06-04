use jni::JNIEnv;
use jni::objects::{GlobalRef, JMethodID};

use std::mem;

use JniResult;

pub struct PairIter<InnerIter: Iterator> {
    pub iter: InnerIter,
    pub element_class: GlobalRef,
    pub constructor_id: JMethodID<'static>,
}

impl<InnerIter: Iterator> PairIter<InnerIter> {
    pub fn new(env: &JNIEnv, iter: InnerIter, class_name: &str) -> JniResult<Self> {
        let class = env.find_class(class_name)?;
        let element_class = env.new_global_ref(class.into())?;
        let signature = "([B[B)V";
        let id = env.get_method_id(class_name, "<init>", signature)?;
        Ok(PairIter {
            iter,
            element_class,
            constructor_id: unsafe { mem::transmute(id) },
        })
    }
}
