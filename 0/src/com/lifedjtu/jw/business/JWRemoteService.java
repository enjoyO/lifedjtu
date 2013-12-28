
package com.lifedjtu.jw.business;


import java.util.List;

import com.lifedjtu.jw.pojos.Area;
import com.lifedjtu.jw.pojos.Building;
import com.lifedjtu.jw.pojos.Room;
import com.lifedjtu.jw.pojos.dto.BuildingDto;
import com.lifedjtu.jw.pojos.dto.CourseDto;
import com.lifedjtu.jw.pojos.dto.DjtuDate;
import com.lifedjtu.jw.pojos.dto.ExamDto;
import com.lifedjtu.jw.pojos.dto.RoomDto;
import com.lifedjtu.jw.pojos.dto.ScoreDto;
import com.lifedjtu.jw.pojos.dto.StudentRegistry;

public interface JWRemoteService {
	public String signinRemote(String studentId, String password);
	
	public StudentRegistry fetchStudentRegistry(String sessionId);
	
	public boolean changeRemotePassword(String sessionId, String originPass, String newPass, String newPassAgain);
	
	public BuildingDto queryBuildingOnDate(String sessionId, int aid, int buildingId, int week, int weekday);
	
	public RoomDto queryRoom(String sessionId, int aid, int buildingId, int roomId);
	
	public List<CourseDto> queryRemoteCourseTable(String sessionId);
	
	public List<ExamDto> queryRemoteExams(String sessionId);
	
	public List<ScoreDto> queryRemoteScores(String sessionId);
	
	public DjtuDate queryDjtuDate(String sessionId);
	
	/**
	 * 服务于内部的Remote方法
	 */
	public List<Area> queryRemoteAreas(String sessionId);
	public List<Building> queryRemoteBuildings(String session, String areaRemoteId);
	public List<Room> queryRemoteRooms(String sesion, String buildingRemoteId);
	public String randomSessionId();
}
