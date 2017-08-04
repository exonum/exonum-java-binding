use jni::JNIEnv;
use jni::objects::GlobalRef;
use jni::errors::Result;

pub struct PairIter<InnerIter> {
    pub iter: InnerIter,
    pub element_class: GlobalRef,
}

impl<InnerIter> PairIter<InnerIter> {
    pub fn new(env: &JNIEnv, iter: InnerIter, class_name: &str) -> Result<Self> {
        let class = env.find_class(class_name)?;
        let element_class = env.new_global_ref(class.into())?;
        Ok(PairIter {
            iter,
            element_class,
        })
    }
}
