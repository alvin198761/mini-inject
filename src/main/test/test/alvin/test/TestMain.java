package test.alvin.test;

import org.alvin.mini_inject.InjectContext;
import org.alvin.mini_inject.annotations.MiniServiceScan;

@MiniServiceScan("test.alvin.test")
public class TestMain {

	public static void main(String[] args) {
		InjectContext.run(TestMain.class, args);
	}

}
