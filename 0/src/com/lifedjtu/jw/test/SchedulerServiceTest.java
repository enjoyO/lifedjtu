package com.lifedjtu.jw.test;

import java.io.File;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.lifedjtu.jw.business.JWRemoteService;
import com.lifedjtu.jw.business.task.JWUpdateCacheScheduler;

public class SchedulerServiceTest {
	@Test
	public void testRemoteService(){
		char sep = File.separatorChar;
		ApplicationContext ctx=new FileSystemXmlApplicationContext("WebRoot"+sep+"WEB-INF"+sep+"applicationContext.xml");

		JWUpdateCacheScheduler scheduler = (JWUpdateCacheScheduler)ctx.getBean("jwUpdateCacheScheduler");
		JWRemoteService remoteService = (JWRemoteService)ctx.getBean("jwRemoteService");
		String sessionId = remoteService.randomSessionId();
		long start = System.currentTimeMillis();
		scheduler.updateRoomTakenItem(sessionId);
		long end = System.currentTimeMillis();
		System.err.println((end-start)/(double)1000+"s");
//		for(CourseDto course : courses){
//			System.out.println(course.toJSON());
//		}
		//System.out.println(roomDto);
	}
}