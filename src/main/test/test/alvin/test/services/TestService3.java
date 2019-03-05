package test.alvin.test.services;

import org.alvin.mini_inject.annotations.MiniComponent;
import org.alvin.mini_inject.annotations.MiniRun;

@MiniComponent
public class TestService3 {

	public void doPrint() {
		for (int i = 0; i < 10; i++) {
			System.out.println(i + "： TestService3");
		}
	}


	@MiniRun
	public void start() {
		System.out.println("TestService33 --");
	}
}
