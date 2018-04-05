package ${groupId};

import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySchema implements Schema {

  private final View view;

  public MySchema(View view) {
    this.view = checkNotNull(view);
  }
}
