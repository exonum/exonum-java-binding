use jni::JNIEnv;
use jni::objects::GlobalRef;
use jni::errors::Result;

pub struct PairIter<InnerIter> {
    pub iter: InnerIter,
    pub entry: GlobalRef,
}

impl<InnerIter> PairIter<InnerIter> {
    pub fn new(env: &JNIEnv, iter: InnerIter, class_name: &str) -> Result<Self> {
        let class = env.find_class(class_name)?;
        let entry = env.new_global_ref(class.into())?;
        Ok(PairIter { iter, entry })
    }
}
