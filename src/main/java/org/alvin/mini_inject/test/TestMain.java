package org.alvin.mini_inject.test;

import org.alvin.mini_inject.InjectContext;
import org.alvin.mini_inject.annotations.MiniServiceScan;

@MiniServiceScan("org.alvin.mini_inject")
public class TestMain {

	public static void main(String[] args) {
		InjectContext.run(TestMain.class, args);
	}

}
