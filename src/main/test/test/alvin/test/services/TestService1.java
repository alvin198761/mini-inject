package test.alvin.test.services;

import org.alvin.mini_inject.annotations.MiniComponent;
import org.alvin.mini_inject.annotations.MiniRun;

@MiniComponent
public class TestService1 {

	private String name = "alvin";

	public void doTest(String hello) {
		System.out.println(hello + "! " + name);
	}


	@MiniRun
	public void start() {
		System.out.println("TestService1 --");
	}

}
