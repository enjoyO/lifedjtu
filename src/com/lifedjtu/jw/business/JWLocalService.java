package com.lifedjtu.jw.business;

import java.util.List;

import com.lifedjtu.jw.business.support.LocalResult;
import com.lifedjtu.jw.pojos.RoomTakenItem;
import com.lifedjtu.jw.pojos.User;
import com.lifedjtu.jw.pojos.dto.BuildingDto;
import com.lifedjtu.jw.pojos.dto.CourseDto;
import com.lifedjtu.jw.pojos.dto.ExamDto;
import com.lifedjtu.jw.pojos.dto.RoomDto;
import com.lifedjtu.jw.pojos.dto.ScoreDto;
import com.lifedjtu.jw.pojos.dto.StudentRegistry;

public interface JWLocalService {
	/**
	 * 核心业务方法，主要与数据库交互（也有可能与远程交互）
	 */
	public boolean isUserExist(String studentId);
	public User addUser(User user);
	public User signinLocal(String studentId, String password);
	public int changeLocalPassword(String studentId, String dynamicPass, String originPass, String newPass, String newPassAgain);
	public LocalResult<List<RoomTakenItem>> queryFreeRooms(String studentId, String dynamicPass, double longitude, double latitude); //无需走远程，直接与数据库交互即可
	public LocalResult<List<CourseDto>> queryLocalCourseTabel(String studentId,String dynamicPass); //忽略注释.....仅在用户注册时，登录时，和请求刷新时调用，这些数据需存储在app中
	public LocalResult<List<ExamDto>> queryLocalExams(String studentId, String dynamicPass); //忽略注释.....仅在用户注册时，登录时，和请求刷新时调用，这些数据需存储在app中。推送利器！同种课程，一旦有一人查询到考试存在，全部选修此课程的同学都会被推送考试通知
	
	/**
	 * 这些方法负责在用户注册时，初始化用户数据，搜取用户信息
	 * 对于一个用户来说，我们需要在其注册时，一步一步引导用户，并从其身上获取必要的数据，更好地为他和为其他用户服务
	 * 这些数据包括：
	 * 1. 原生的用户个人信息，需要从获取学籍处获得
	 * 2. 原生的用户课程信息，需要从个人课表处获得，数据会被加载，加载后分别刷新Course表和CourseInstance表
	 * 3. 原生的用户考试信息，需要从个人考试处获得，数据会被加载，加载后分别刷新Exam表和ExamInstance表
	 */
	public LocalResult<User> fetchUserDetailInfo(String studentId, String dynamicPass); //加载用户详细信息，更新数据库，返回User实体
	public int fetchUserCourseInfo(String studentId, String dynamicPass); //获取用户课程信息，刷新Course表格，填充属于此用户的CourseInstance，迅速返回此用户的查询结果，异步调用数据库更新方法。
	public int fetchUserExamInfo(String studentId, String dynamicPass); //获取用户考试信息，刷新Exam表格，填充属于此用户的ExamInstance，迅速返回此用户的查询结果，异步调用数据库更新方法。
	
	
	/**
	 * 以下方法的返回结果，或从remoteService获取的中间变量不存数据库
	 */
	public LocalResult<StudentRegistry> fetchStudentRegistry(String studentId, String dynamicPass);
	public LocalResult<BuildingDto> queryBuildingOnDate(String studentId,String dynamicPass, int aid, int buildingId, int week, int weekday);
	public LocalResult<RoomDto> queryRoom(String studentId,String dynamicPass, int aid, int buildingId, int roomId);
	public LocalResult<List<ScoreDto>> queryLocalScores(String studentId, String dynamicPass); //忽略注释....推送利器！同种课程实例！注意是实例，一旦有一人查询到分数已出，全部在读于这一课程实例（同一老师授课）的同学都会收到出分推送通知！
	
	
	/**
	 * 以下为一些工具类服务方法,仅限内部调用
	 */
	public boolean checkPrivateKey(String studentId, String dynamicPass);
	
}