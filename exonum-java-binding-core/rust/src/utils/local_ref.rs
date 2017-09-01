use jni::JNIEnv;
use jni::objects::JObject;
use jni::sys::jobject;

pub struct AutoLocalRef<'a> {
    object: jobject,
    env: &'a JNIEnv<'a>,
}

impl<'a> AutoLocalRef<'a> {
    pub fn new(env: &'a JNIEnv<'a>, object: jobject) -> Self {
        Self { object, env }
    }

    pub fn as_obj(&self) -> JObject {
        self.object.into()
    }
}

impl<'a> Drop for AutoLocalRef<'a> {
    fn drop(&mut self) {
        if let Err(e) = self.env.delete_local_ref(self.object.into()) {
            error!("Unable to delete local reference: {}", e.description());
        }
    }
}
