import com.exonum.binding.service.ServiceAdapterMultiFactory;

public class Run {
  public static void main(String[] args) {
    ServiceAdapterMultiFactory f = ServiceAdapterMultiFactory.getInstance();

    System.out.println("As a SPI-based service: " + f.createService());
    System.out.println("As a reflective service: "
        + f.createService("com.exonum.binding.service1.MyServiceFactory"));
  }
}
