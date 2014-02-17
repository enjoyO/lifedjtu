package com.lifedjtu.jw.business.task.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lifedjtu.jw.business.JWRemoteService;
import com.lifedjtu.jw.business.task.JWUpdateCacheAsyncer;
import com.lifedjtu.jw.dao.CriteriaWrapper;
import com.lifedjtu.jw.dao.impl.CourseDao;
import com.lifedjtu.jw.dao.impl.CourseInstanceDao;
import com.lifedjtu.jw.dao.impl.ExamDao;
import com.lifedjtu.jw.dao.impl.ExamInstanceDao;
import com.lifedjtu.jw.dao.impl.SystemNoticeDao;
import com.lifedjtu.jw.dao.impl.UserCourseDao;
import com.lifedjtu.jw.dao.support.UUIDGenerator;
import com.lifedjtu.jw.pojos.Course;
import com.lifedjtu.jw.pojos.CourseInstance;
import com.lifedjtu.jw.pojos.Exam;
import com.lifedjtu.jw.pojos.ExamInstance;
import com.lifedjtu.jw.pojos.SystemNotice;
import com.lifedjtu.jw.pojos.User;
import com.lifedjtu.jw.pojos.UserCourse;
import com.lifedjtu.jw.pojos.dto.CourseDto;
import com.lifedjtu.jw.pojos.dto.CourseTakenItem;
import com.lifedjtu.jw.pojos.dto.DjtuDate;
import com.lifedjtu.jw.pojos.dto.ExamDto;
import com.lifedjtu.jw.pojos.dto.ExamTimeEntry;
import com.lifedjtu.jw.pojos.dto.ScoreDto;
import com.lifedjtu.jw.util.MapMaker;
import com.lifedjtu.jw.util.pattern.InfoProcessHub;

@Component("jwUpdateCacheAsyncer")
@Transactional
public class JWUpdateCacheAsyncerImpl implements JWUpdateCacheAsyncer{
	@Autowired
	private CourseDao courseDao;
	@Autowired
	private CourseInstanceDao courseInstanceDao;
	@Autowired
	private ExamDao examDao;
	@Autowired
	private ExamInstanceDao examInstanceDao;
	@Autowired
	private UserCourseDao userCourseDao;
	@Autowired
	private JWRemoteService jwRemoteService;
	@Autowired
	private SystemNoticeDao systemNoticeDao;
	@Override
	@Async
	public void updateCourseInfo(String userId, List<CourseDto> courseDtos, DjtuDate djtuDate) {
		try{
			List<Course> courses = new ArrayList<Course>();
			List<CourseInstance> courseInstances = new ArrayList<CourseInstance>();
			List<UserCourse> userCourses = new ArrayList<UserCourse>();
			for(CourseDto courseDto : courseDtos){
				//System.err.println(courseDto.getAliasName());
				Course course = courseDao.findOneByParams(CriteriaWrapper.instance().and(Restrictions.eq("courseAlias", courseDto.getAliasName()),Restrictions.eq("courseName", courseDto.getCourseName())));
				if(course==null){
					course = new Course();
					course.setCourseAlias(courseDto.getAliasName());
					course.setCourseName(courseDto.getCourseName());
					course.setId(UUIDGenerator.randomUUID());
					course.setCourseCredits(Double.parseDouble(courseDto.getCourseMarks().trim()));
					course.setCourseProperty(courseDto.getCourseAttr());
					courses.add(course);
				}
				CourseInstance courseInstance = updateCourseInstanceInfo(course, courseDto, djtuDate.getSchoolYear(), djtuDate.getTerm());
				courseInstances.add(courseInstance);
				UserCourse userCourse = userCourseDao.findOneByParams(CriteriaWrapper.instance().and(Restrictions.eq("user.id", userId),Restrictions.eq("courseInstance.id", courseInstance.getId())));
				if(userCourse==null){
					userCourse = new UserCourse();
					userCourse.setId(UUIDGenerator.randomUUID());
					userCourse.setUser(new User(userId));
					userCourse.setCourseInstance(courseInstance);
					userCourses.add(userCourse);
				}
				
			}
			courseDao.addMulti(courses);
			courseInstanceDao.addMulti(courseInstances);
			userCourseDao.addMulti(userCourses);
		}catch(Exception exception){
			exception.printStackTrace();
		}
		
	}

	@Override
	public CourseInstance updateCourseInstanceInfo(Course course, CourseDto courseDto, int year, int term) {
		CourseInstance courseInstance = courseInstanceDao.findOneByParams(CriteriaWrapper.instance().and(Restrictions.eq("courseAlias", courseDto.getAliasName()),Restrictions.eq("courseRemoteId", courseDto.getCourseRemoteId()),Restrictions.eq("courseName", courseDto.getCourseName())));
		if(courseInstance==null){
			courseInstance = new CourseInstance();
			courseInstance.setId(UUIDGenerator.randomUUID());
			courseInstance.setCourseAlias(courseDto.getAliasName());
			courseInstance.setCourseName(courseDto.getCourseName());
			courseInstance.setCourseRemoteId(courseDto.getCourseRemoteId());
			courseInstance.setBadEval(0);
			courseInstance.setGoodEval(0);
			courseInstance.setExamStatus(courseDto.getExamAttr());
			courseInstance.setCourseSequence(courseDto.getCourseNumber());
			courseInstance.setPostponed(courseDto.isDelayed());
			courseInstance.setCourse(course);
		}
		

		
		StringBuilder takenBuilder = new StringBuilder();
		List<CourseTakenItem> courseTakenItems = courseDto.getCourseTakenItems();
		if(courseTakenItems==null||courseTakenItems.size()==0){
			takenBuilder.append("时间地点均不占");
		}else{
			for(CourseTakenItem courseTakenItem : courseTakenItems){
				takenBuilder.append(InfoProcessHub.transferCourseTakenItem(courseTakenItem)+";");
			}
		}
		
		courseInstance.setCourseTakenInfo(takenBuilder.toString());
		courseInstance.setTeacherName(courseDto.getTeacherName());
		courseInstance.setYear(year);
		courseInstance.setTerm(term);
		
		return courseInstance;
	}

	@Override
	@Async
	public void updateExamInfo(String userId, List<ExamDto> examDtos, DjtuDate djtuDate){
		try{
			List<SystemNotice> notices = new ArrayList<SystemNotice>();
			
			for(ExamDto examDto : examDtos){
				Course course = courseDao.findOneByParams(CriteriaWrapper.instance().and(Restrictions.eq("courseName", examDto.getCourseName()),Restrictions.eq("courseAlias", examDto.getCourseAliasName())));
				Exam exam = examDao.findOneByParams(CriteriaWrapper.instance().and(Restrictions.eq("course.id", course.getId())));
				if(exam==null){
					exam = new Exam();
					exam.setCourse(course);
					exam.setCourseAlias(examDto.getCourseAliasName());
					exam.setCourseName(examDto.getCourseName());
					exam.setId(UUIDGenerator.randomUUID());
				}
				
				examDao.add(exam);
				
				
				
				
				//examDate日期，roomName考试地点，courseProperty考试性质，course的全部信息，根据这些确定一个唯一的courseInstance
				//courseAlias，examStatus，year，term
				UserCourse userCourse = userCourseDao.findOneByJoinedParams(MapMaker.instance("courseInstance", "courseInstance").toMap(),CriteriaWrapper.instance().and(Restrictions.eq("user.id", userId),Restrictions.eq("courseInstance.year", djtuDate.getSchoolYear()), Restrictions.eq("courseInstance.term", djtuDate.getTerm()),Restrictions.eq("courseInstance.examStatus", examDto.getCourseProperty()), Restrictions.eq("courseInstance.courseAlias", course.getCourseAlias())));
				userCourse.setExamNoted(true);
				CourseInstance courseInstance = userCourse.getCourseInstance();				
				ExamInstance examInstance = updateExamInstanceInfo(exam, examDto, courseInstance);
				//勿忘set上新更新的ExamInstance
				userCourse.setExamInstance(examInstance);
				userCourseDao.update(userCourse);
				
				//开始两种关联通知！第一种更新和第二种更新均需要跟此用户与此考试的科目关联的CourseInstance
				//1. 通知直接用户
				List<UserCourse> userCourses = userCourseDao.findByParams(CriteriaWrapper.instance().and(Restrictions.eq("courseInstance.id", courseInstance.getId())));
				for(UserCourse uc : userCourses){
					//uc.setExamInstance(examInstance);
					if(!uc.isExamNoted()){
						SystemNotice notice = new SystemNotice();
						notice.setId(UUIDGenerator.randomUUID());
						notice.setDate(new Date());
						notice.setToUser(uc.getUser());
						notice.setTitle("考试安排通知");
						notice.setContent(examDto.getCourseName()+"("+examDto.getCourseAliasName()+")的考试安排已经新鲜出炉,快来登陆人在交大查看考场教室,我们祝福你考一个好成绩哦~");
						notices.add(notice);
						uc.setExamNoted(true);
					}
				}
				userCourseDao.addMulti(userCourses);
				
				//2. 通知间接用户
				List<UserCourse> userCourses2 = userCourseDao.findByJoinedParams(MapMaker.instance("courseInstance", "courseInstance").toMap(),CriteriaWrapper.instance().and(Restrictions.eq("courseInstance.courseAlias", courseInstance.getCourseAlias()),Restrictions.eq("courseInstance.year", djtuDate.getSchoolYear()), Restrictions.eq("courseInstance.term", djtuDate.getTerm()),Restrictions.eq("courseInstance.examStatus", examDto.getCourseProperty()),Restrictions.ne("courseInstance.id", courseInstance.getId())));
				//List<ExamInstance> examInstances = new ArrayList<ExamInstance>();
				for(UserCourse uc : userCourses2){
					
					if(!uc.isExamNoted()){
						SystemNotice notice = new SystemNotice();
						notice.setId(UUIDGenerator.randomUUID());
						notice.setDate(new Date());
						notice.setToUser(uc.getUser());
						notice.setTitle("考试安排通知");
						notice.setContent(examDto.getCourseName()+"("+examDto.getCourseAliasName()+")的考试安排已经新鲜出炉,快来登陆人在交大查看考场教室,我们祝福你考一个好成绩哦~");
						notices.add(notice);
						uc.setExamNoted(true);
					}
					
//					ExamInstance tempInstance = uc.getExamInstance();
//					if(tempInstance==null){
//						tempInstance = new ExamInstance();
//						tempInstance.setCourseInstance(uc.getCourseInstance());
//						tempInstance.setCourseName(uc.getCourseInstance().getCourseName());
//						tempInstance.setExam(exam);
//						tempInstance.setId(UUIDGenerator.randomUUID());
//						tempInstance.setScoreOut(false);
//						tempInstance.setExamDate(exam.getExamDate());
//						tempInstance.setLastedMinutes(exam.getLastedMinutes());
//						tempInstance.setRoomName(null);
//						examInstances.add(tempInstance);
//					}else if(tempInstance.getRoomName()==null||tempInstance.getRoomName().equals("")){
//						tempInstance.setRoomName(null);
//						tempInstance.setExamDate(exam.getExamDate());
//						tempInstance.setLastedMinutes(exam.getLastedMinutes());
//						examInstances.add(tempInstance);
//					}else{
//						continue;
//					}
//					uc.setExamInstance(tempInstance);
				}
				//examInstanceDao.addMulti(examInstances);
				userCourseDao.addMulti(userCourses2);
			}
		
			systemNoticeDao.addMulti(notices);
			
		}catch(Exception exception){
			exception.printStackTrace();
			
		}
	}

	@Override
	public ExamInstance updateExamInstanceInfo(Exam exam, ExamDto examDto, CourseInstance courseInstance){
		//timeEntry是一个考试实例的时间
		ExamTimeEntry timeEntry = null;
		try {
			timeEntry = InfoProcessHub.transferExamDate(examDto.getExamDate());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		ExamInstance examInstance = examInstanceDao.findOneByParams(CriteriaWrapper.instance().and(Restrictions.eq("roomName", examDto.getRoomName()),Restrictions.eq("examStatus", examDto.getCourseProperty()),Restrictions.eq("courseInstance.id", courseInstance.getId())));
		
		if(examInstance==null){
			examInstance = new ExamInstance();
			examInstance.setCourseInstance(courseInstance);
			examInstance.setCourseName(courseInstance.getCourseName());
			examInstance.setExam(exam);
			examInstance.setId(UUIDGenerator.randomUUID());
			examInstance.setScoreOut(false);
		}
		examInstance.setExamDate(timeEntry.getDate());
		examInstance.setExamStatus(examDto.getCourseProperty());
		examInstance.setLastedMinutes(timeEntry.getLastedMinutes());
		examInstance.setRoomName(examDto.getRoomName());
		examInstanceDao.add(examInstance);
		return examInstance;
	}

	
	@Override
	@Async
	public void updateScoreOutInfo(String studentId, List<ScoreDto> scoreDtos,
			DjtuDate djtuDate) {
		
		try{
			List<SystemNotice> notices = new ArrayList<SystemNotice>();
			
			for(ScoreDto scoreDto : scoreDtos){
				UserCourse userCourse = userCourseDao.findOneByJoinedParams(MapMaker.instance("courseInstance", "courseInstance").param("user", "user").toMap(),CriteriaWrapper.instance().and(Restrictions.eq("user.studentId", studentId),Restrictions.eq("courseInstance.year", djtuDate.getSchoolYear()), Restrictions.eq("courseInstance.term", djtuDate.getTerm()),Restrictions.eq("courseInstance.examStatus", scoreDto.getCourseProperty()), Restrictions.eq("courseInstance.courseAlias", scoreDto.getCourseAliasName())));
				userCourse.setScoreNoted(true);
				CourseInstance courseInstance = userCourse.getCourseInstance();
				List<UserCourse> userCourses = courseInstance.getUserCourses();
				for(UserCourse uc : userCourses){
					if(!uc.isScoreNoted()){
						SystemNotice notice = new SystemNotice();
						notice.setId(UUIDGenerator.randomUUID());
						notice.setDate(new Date());
						notice.setToUser(uc.getUser());
						notice.setTitle("考试出分通知");
						notice.setContent(scoreDto.getCourseName()+"("+scoreDto.getCourseAliasName()+")的考试分数已经新鲜出炉,快来登陆人在交大查看成绩吧~");
						notices.add(notice);
						uc.setScoreNoted(true);
					}
				}
				
				userCourseDao.addMulti(userCourses);
			}
			
			systemNoticeDao.addMulti(notices);
		}catch(Exception exception){
			exception.printStackTrace();
			
		}
		
	}

	
	public CourseDao getCourseDao() {
		return courseDao;
	}

	public void setCourseDao(CourseDao courseDao) {
		this.courseDao = courseDao;
	}

	public CourseInstanceDao getCourseInstanceDao() {
		return courseInstanceDao;
	}

	public void setCourseInstanceDao(CourseInstanceDao courseInstanceDao) {
		this.courseInstanceDao = courseInstanceDao;
	}

	public ExamDao getExamDao() {
		return examDao;
	}

	public void setExamDao(ExamDao examDao) {
		this.examDao = examDao;
	}

	public ExamInstanceDao getExamInstanceDao() {
		return examInstanceDao;
	}

	public void setExamInstanceDao(ExamInstanceDao examInstanceDao) {
		this.examInstanceDao = examInstanceDao;
	}
	
	public JWRemoteService getJwRemoteService() {
		return jwRemoteService;
	}

	public void setJwRemoteService(JWRemoteService jwRemoteService) {
		this.jwRemoteService = jwRemoteService;
	}

	public UserCourseDao getUserCourseDao() {
		return userCourseDao;
	}

	public void setUserCourseDao(UserCourseDao userCourseDao) {
		this.userCourseDao = userCourseDao;
	}

	public SystemNoticeDao getSystemNoticeDao() {
		return systemNoticeDao;
	}

	public void setSystemNoticeDao(SystemNoticeDao systemNoticeDao) {
		this.systemNoticeDao = systemNoticeDao;
	}

	
	
	
}
