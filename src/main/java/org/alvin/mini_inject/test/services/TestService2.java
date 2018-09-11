package org.alvin.mini_inject.test.services;

import org.alvin.mini_inject.annotations.MiniInject;
import org.alvin.mini_inject.annotations.MiniComponent;
import org.alvin.mini_inject.annotations.MiniRun;

@MiniComponent
public class TestService2 {

	@MiniInject
	private TestService1 testService1;
	@MiniInject
	private TestService3 testService3;

	public void doInject() {
		this.testService1.doTest("你好：");
		this.testService1.doTest("我很讨厌你：");
		//
		this.testService3.doPrint();
	}

	@MiniRun
	public void start() {
		System.out.println("TestService2 --");
		this.doInject();
	}
}
