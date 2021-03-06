
package com.lifedjtu.jw.business.impl;


import java.util.*;

import javax.servlet.ServletContext;

import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import com.lifedjtu.jw.business.JWRemoteService;
import com.lifedjtu.jw.config.LifeDjtuConfig;
import com.lifedjtu.jw.pojos.Area;
import com.lifedjtu.jw.pojos.Building;
import com.lifedjtu.jw.pojos.Room;
import com.lifedjtu.jw.pojos.dto.ArticleDto;
import com.lifedjtu.jw.pojos.dto.BuildingDto;
import com.lifedjtu.jw.pojos.dto.CourseDto;
import com.lifedjtu.jw.pojos.dto.CourseRecordDto;
import com.lifedjtu.jw.pojos.dto.CourseTakenItem;
import com.lifedjtu.jw.pojos.dto.DjtuDate;
import com.lifedjtu.jw.pojos.dto.ExamDto;
import com.lifedjtu.jw.pojos.dto.RoomDayInfo;
import com.lifedjtu.jw.pojos.dto.RoomDto;
import com.lifedjtu.jw.pojos.dto.RoomInfoDto;
import com.lifedjtu.jw.pojos.dto.RoomWeekInfo;
import com.lifedjtu.jw.pojos.dto.ScoreDto;
import com.lifedjtu.jw.pojos.dto.StudentRegistry;
import com.lifedjtu.jw.util.fetcher.support.FetchResponse;
import com.lifedjtu.jw.util.LifeDjtuUtil;
import com.lifedjtu.jw.util.MapMaker;
import com.lifedjtu.jw.util.fetcher.URLFetcher;
import com.lifedjtu.jw.util.extractor.DomElement;
import com.lifedjtu.jw.util.extractor.Extractor;
import com.lifedjtu.jw.util.pattern.InfoProcessHub;

import static com.lifedjtu.jw.util.extractor.Extractor.$;

@Component("jwRemoteService")
public class JWRemoteServiceImpl implements JWRemoteService,ServletContextAware {

	//private final String loginURL = "202.199.128.21/academic/j_acegi_security_check";
	private static final String studentMessageURL = "http://202.199.128.21/academic/showHeader.do";
	private static final String studentRegistryURL = "http://202.199.128.21/academic/student/studentinfo/studentInfoModifyIndex.do?frombase=0&wantTag=0";
	private static final String modifyPasswordURL = "http://202.199.128.21/academic/sysmgr/modifypasswd_user.jsdo";
	private static final String queryBuildingOnDateURL = "http://202.199.128.21/academic/teacher/teachresource/roomschedule_week.jsdo";
	private static final String queryRoomURL = "http://202.199.128.21/academic/teacher/teachresource/roomschedule.jsdo";
	private static final String queryRemoteCourseTableURL = "http://202.199.128.21/academic/student/currcourse/currcourse.jsdo";
	private static final String queryRemoteExamsURL = "http://202.199.128.21/academic/manager/examstu/studentQueryAllExam.do?pagingPageVLID=1&sortDirectionVLID=-1&pagingNumberPerVLID=30&sortColumnVLID=examRoom.exam.endTime";
	private static final String queryRemoteScoresURL = "http://202.199.128.21/academic/manager/score/studentOwnScore.do";
	private static final String queryAreaURL = "http://202.199.128.21/academic/teacher/teachresource/roomschedulequery.jsdo";
	private static final String queryDateURL = "http://202.199.128.21/academic/listLeft.do";

    private static final String imServerUrl = LifeDjtuConfig.getProperty("djtu.imServer");
	private static final String loginCrackCodeURL = imServerUrl + (imServerUrl.endsWith("/")?"":"/") + "fetchCodeAndSessionId/";
	
	private static final String queryNoteURL = "http://202.199.128.21/jwzx/infoArticleList.do?columnId=259&sortColumn=publicationDate&sortDirection=-1&pagingNumberPer=15";

	//查询单门课程教学记录
	private static final String queryCourseRecord = "http://202.199.128.21/academic/teacher/teachingtask/recordStudentIndex.do?";
	
	
	private ServletContext servletContext;
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


    @Override
    public String showHeader(String sessionId) {
        FetchResponse fetchResponse = URLFetcher.fetchURLByGet(studentMessageURL,sessionId);
        //System.out.println(fetchResponse.getResponseBody());
        return $("#greeting", fetchResponse.getResponseBody()).get(0).getText().trim();
    }

    @Override
	public String signinRemote(String studentId, String password) {

		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(loginCrackCodeURL+studentId+"?randomCode="+password, null);

        System.out.println("sign in remote...");

        if(fetchResponse.getResponseBody().trim().length()<20){
			System.out.println("Login Failed:\t"+fetchResponse.getResponseBody());
			return null;
		}else {
			return fetchResponse.getResponseBody().trim();
		}
		
	}

	@Override
	public StudentRegistry fetchStudentRegistry(String sessionId) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(studentRegistryURL, sessionId);
		//System.out.println(fetchResponse.getResponseBody());
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> title = Extractor.$("table[class=form] th",fetchResponse.getResponseBody());
		List<DomElement> value = Extractor.$("table[class=form] td",fetchResponse.getResponseBody());
		List<String> titles = new ArrayList<>();
		List<String> values = new ArrayList<>();
        for (DomElement aTitle : title) {
            titles.add(aTitle.getText().trim());
        }
        for (DomElement aValue : value) {
            values.add(aValue.getText().trim());
        }
		values.remove(2);
		return new StudentRegistry(titles, values);
	}

	@Override
	public boolean changeRemotePassword(String sessionId, String originPass,
			String newPass, String newPassAgain) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByPost(modifyPasswordURL, sessionId, MapMaker.instance("oldpasswd", originPass)
																						.param("newpasswd", newPass)
																						.param("confirmedpasswd", newPassAgain)
																						.toMap());
		//System.err.println(fetchResponse.getResponseBody());
		
		List<DomElement> list = Extractor.$("td[id=content_margin]",fetchResponse.getResponseBody());
        return list.isEmpty();
    }

	@Override
	public BuildingDto queryBuildingOnDate(String sessionId, int aid,
			int buildingId, int week, int weekday) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByPost(queryBuildingOnDateURL, sessionId, MapMaker.instance("aid", Integer.toString(aid))
				.param("buildingid", buildingId+"")
				.param("whichweek", week+"")
				.param("week", weekday+"")
				.param("room", -1+"")
				.toMap());
		//System.out.println(fetchResponse.getResponseBody());

		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("tr[class=infolist_common]",fetchResponse.getResponseBody());
		//System.out.println(list.size());
		List<RoomInfoDto> roomInfoDtos = new ArrayList<>();
        for (DomElement aList : list) {
            List<DomElement> info = aList.children("td");
            List<DomElement> occupyInfo = info.get(6).find("tr").get(1).children("td");
            List<String> occupyInfos = new ArrayList<>();
            for (DomElement anOccupyInfo : occupyInfo) {
                //System.out.println(occupyInfo.get(j).getText().trim());
                occupyInfos.add(anOccupyInfo.getText().trim());
            }
            RoomInfoDto roomInfoDto = new RoomInfoDto(info.get(0).getText().trim()
                    , info.get(1).getText().trim()
                    , info.get(2).getText().trim()
                    , info.get(3).getText().trim()
                    , info.get(4).getText().trim()
                    , info.get(5).getText().trim()
                    , occupyInfos);
            roomInfoDtos.add(roomInfoDto);
        }
		return new BuildingDto(roomInfoDtos);
	}

	@Override
	public RoomDto queryRoom(String sessionId, int aid, int buildingId, int roomId) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByPost(queryRoomURL, sessionId, MapMaker.instance("aid", Integer.toString(aid))
																					.param("buildingid", Integer.toString(buildingId))
																					.param("room", Integer.toString(roomId))
																					.toMap());
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> table = Extractor.$("table[class=infolists_tab]",fetchResponse.getResponseBody());
		//System.out.println(fetchResponse.getResponseBody());
		List<RoomWeekInfo> weekInfos = new ArrayList<>();
        for (DomElement aTable : table) {
            List<RoomDayInfo> dayInfos = new ArrayList<>();
            List<List<String>> week = new ArrayList<>();
            List<DomElement> list = aTable.children("tr");
            for (int j = 1; j < list.size(); j++) {
                List<String> classes = new ArrayList<>();
                List<DomElement> node = list.get(j).children("td");
                for (int k = 1; k < node.size(); k++) {
                    classes.add(node.get(k).getText().trim());
                }
                week.add(classes);
            }
            for (int p = 0; p < 7; p++) {
                List<String> infos = new ArrayList<>();
                for (int q = 0; q < 10; q++) {

                    infos.add(week.get(q).get(p));
                }
                RoomDayInfo dayInfo = new RoomDayInfo(infos);
                dayInfos.add(dayInfo);
            }
            RoomWeekInfo weekInfo = new RoomWeekInfo(dayInfos);
            weekInfos.add(weekInfo);
        }
		return  new RoomDto(weekInfos);
		
	}

	@Override
	public List<CourseDto> queryRemoteCourseTable(String sessionId) {
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryRemoteCourseTableURL, sessionId);
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		//System.out.println(fetchResponse.getResponseBody());
		List<DomElement> list = Extractor.$("table[class=infolist_tab]:first > tr[class=infolist_common]",fetchResponse.getResponseBody());
		List<CourseDto> courses = new ArrayList<>();
        for (DomElement aList : list) {
            List<DomElement> courseTemp = aList.children("td");
            List<DomElement> courseTakenItemTemp = courseTemp.get(9).find("tr");
            List<CourseTakenItem> courseTakenItems = new ArrayList<>();
            for (DomElement aCourseTakenItemTemp : courseTakenItemTemp) {
                List<DomElement> taken = aCourseTakenItemTemp.children("td");
                //System.out.println(taken.get(0).getText()+"  "+taken.get(1).getText());
                CourseTakenItem item = new CourseTakenItem(taken.get(0).getText().trim(),
                        taken.get(1).getText().trim(),
                        taken.get(2).getText().trim(),
                        taken.get(3).getText().trim());
                courseTakenItems.add(item);
            }

            String epidIdHref = courseTemp.get(11).find("a").get(0).attr("href");


            CourseDto course = new CourseDto(courseTemp.get(0).getText().trim(),
                    courseTemp.get(1).getText().trim(),
                    courseTemp.get(2).getText().trim(),
                    courseTemp.get(3).getText().trim(),
                    courseTemp.get(4).getText().trim(),
                    courseTemp.get(5).getText().trim(),
                    courseTemp.get(6).getText().trim(),
                    courseTemp.get(7).getText().trim(),
                    courseTemp.get(8).getText().trim(),
                    epidIdHref.substring(epidIdHref.lastIndexOf("=") + 1),
                    courseTakenItems);
            courses.add(course);
        }
		return courses;
	}

	@Override
	public List<ExamDto> queryRemoteExams(String sessionId) {
		DjtuDate djtuDate = queryDjtuDate();
		
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryRemoteExamsURL, sessionId);
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("table[class=datalist] tr",fetchResponse.getResponseBody());
		List<ExamDto> exams = new ArrayList<>();
		for(int i=1;i<list.size();i++){
			List<DomElement> examTemp = list.get(i).children("td");
			
			//filter date
			String dateStr = examTemp.get(2).getText().trim().split("\\s")[0];
			Date date = LifeDjtuUtil.parseDate(dateStr);
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			
//			int examYear = calendar.get(Calendar.YEAR);
//			int examMonth = calendar.get(Calendar.MONTH)+1;
//			int examDay = calendar.get(Calendar.DAY_OF_MONTH);
			
			Date curStart = LifeDjtuUtil.getStartDateOfTerm(djtuDate.getSchoolYear(), djtuDate.getTerm());
			Date nextStart;
			if(djtuDate.getTerm()==1){
				nextStart = LifeDjtuUtil.getStartDateOfTerm(djtuDate.getSchoolYear(), djtuDate.getTerm()+1);
			}else{
				nextStart = LifeDjtuUtil.getStartDateOfTerm(djtuDate.getSchoolYear()+1, djtuDate.getTerm());
			}
			
			//2 and 8
			if(date.getTime() < curStart.getTime() || date.getTime() >= nextStart.getTime()){
				continue;
			}
			
			
			
			ExamDto exam = new ExamDto(examTemp.get(0).getText().trim(),
					examTemp.get(1).getText().trim(),
					examTemp.get(2).getText().trim(),
					examTemp.get(3).getText().trim(),
					examTemp.get(4).getText().trim());
			exams.add(exam);
		}
		return exams;
	}

	@Override
	public List<ScoreDto> queryRemoteScores(String sessionId) {
		//System.err.println("hello you!!!in remote scores");

		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryRemoteScoresURL, sessionId);
		//System.err.println(fetchResponse.getResponseBody());
		
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("table[class=datalist] > tr",fetchResponse.getResponseBody());
		List<ScoreDto> scores = new ArrayList<>();
		for(int i=1;i<list.size();i++){
			List<DomElement> scoreTemp = list.get(i).children("td");
			ScoreDto score = new ScoreDto(scoreTemp.get(0).getText().trim(),
					scoreTemp.get(1).getText().trim(),
					scoreTemp.get(2).getText().trim(),
					scoreTemp.get(3).getText().trim(),
					scoreTemp.get(4).getText().trim(),
					scoreTemp.get(5).getText().trim(),
					scoreTemp.get(6).getText().trim(),
					scoreTemp.get(7).getText().trim(),
					scoreTemp.get(8).getText().trim(),
					scoreTemp.get(9).getText().trim(),
					scoreTemp.get(10).getText().trim(),
					scoreTemp.get(11).getText().trim(),
					scoreTemp.get(12).getText().trim(),
					scoreTemp.get(13).getText().trim(),
					scoreTemp.get(14).getText().trim(),
					scoreTemp.get(15).getText().trim());
			scores.add(score);
		}
		return scores;
	}

	@Override
	public List<Area> queryRemoteAreas(String sessionId) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryAreaURL, sessionId);
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("table[class=broken_tab] > tr",fetchResponse.getResponseBody());
		List<DomElement> area = list.get(0).children("td").get(1).children("select").get(0).children("option");
		System.out.println(area.size());
		List<Area> areas = new ArrayList<>();
		for(int i=1;i<area.size();i++){
			Area tempArea = new Area();
			tempArea.setAreaRemoteId(new Integer(area.get(i).getAttributes().get("value")));
			tempArea.setAreaName(area.get(i).getText().trim());
			areas.add(tempArea);
		}
		return areas;
	}

	@Override
	public List<Building> queryRemoteBuildings(String sessionId,
			String areaRemoteId) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByPost(queryAreaURL, sessionId, MapMaker.instance("aid", areaRemoteId).toMap());
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("table[class=broken_tab] > tr",fetchResponse.getResponseBody());
		List<DomElement> building = list.get(0).children("td").get(3).children("select").get(0).children("option");
		List<Building> buildings = new ArrayList<>();
		for(int i=1;i<building.size();i++){
			Building tempBuilding = new Building();
			tempBuilding.setBuildingRemoteId(new Integer(building.get(i).getAttributes().get("value")));
			tempBuilding.setBuildingName(building.get(i).getText().trim());
			buildings.add(tempBuilding);
		}
		return buildings;
	}

	@Override
	public List<Room> queryRemoteRooms(String sessionId, String buildingRemoteId) {
		// TODO Auto-generated method stub
		FetchResponse fetchResponse = URLFetcher.fetchURLByPost(queryAreaURL, sessionId, MapMaker.instance("buildingid", buildingRemoteId).toMap());
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("table[class=broken_tab] > tr",fetchResponse.getResponseBody());
		List<DomElement> room = list.get(1).children("td").get(1).children("select").get(0).children("option");
		List<Room> rooms = new ArrayList<>();
		for(int i=1;i<room.size();i++){
			Room tempRoom = new Room();
			tempRoom.setRoomRemoteId(new Integer(room.get(i).getAttributes().get("value")));
			tempRoom.setRoomName(room.get(i).getText().trim());
			rooms.add(tempRoom);
		}
		//System.out.println(rooms.size());
		fetchResponse = URLFetcher.fetchURLByPost(queryBuildingOnDateURL, sessionId, MapMaker.instance("buildingid", buildingRemoteId)
																					.param("whichweek", "1")
																					.param("week", "1").toMap());
		list = Extractor.$("tr[class=infolist_common]",fetchResponse.getResponseBody());
		//room = list.get(0).children("tr");
		//System.out.println(fetchResponse.getResponseBody());
		//System.out.println(list.size());
		for(int i=0;i<list.size();i++){
			room = list.get(i).children("td");
			rooms.get(i).setRoomSeatNum(new Integer(room.get(1).getText().trim()));
			rooms.get(i).setCourseCapacity(new Integer(room.get(2).getText().trim()));
			rooms.get(i).setExamCapacity(new Integer(room.get(3).getText().trim()));
			rooms.get(i).setRoomSeatType(room.get(4).getText().trim());
			rooms.get(i).setRoomType(room.get(5).getText().trim());
		}
		return rooms;
	}

	public DjtuDate queryDjtuDate() {

        DjtuDate djtuDate = (DjtuDate)servletContext.getAttribute("djtuDate");
        boolean eagerQuery = false;

        if(djtuDate==null){
            eagerQuery = true;
        }else{
            Date date = new Date();
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(date);

            if(djtuDate.getYear()!=calendar.get(Calendar.YEAR)||djtuDate.getMonth()!=calendar.get(Calendar.MONTH)||djtuDate.getDay()!=calendar.get(Calendar.DAY_OF_MONTH)){
                eagerQuery = true;
            }

        }

        if(eagerQuery){
            FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryDateURL, null);
            if(fetchResponse.getStatusCode()!=200||!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
                return null;
            }
            List<DomElement> domElements = Extractor.$("#date:first", fetchResponse.getResponseBody());
            if(domElements==null||domElements.size()==0){
                return null;
            }
            DomElement domElement = domElements.get(0);
            String text = domElement.getText().trim();


            djtuDate =  InfoProcessHub.analyseDjtuDate(text);
        }

		return djtuDate;
	}
	
	@Override
	public String randomSessionId() {
		
		String globalSession = (String)servletContext.getAttribute("global_session");
		Long globalSessionAge = (Long)servletContext.getAttribute("global_session_age");
		
		boolean refetchFlag = false;
		
		if(globalSession==null||globalSessionAge==null){
			refetchFlag = true;
		}else{
			if(System.currentTimeMillis()-globalSessionAge>=LifeDjtuConfig.getIntegerProperty("djtu.sessionAge")){
				refetchFlag = true;
			}
		}
		
		if(refetchFlag){
			globalSession = signinRemote(LifeDjtuConfig.getProperty("remote.studentId"), LifeDjtuConfig.getProperty("remote.password"));
			globalSessionAge = System.currentTimeMillis();
			servletContext.setAttribute("global_session", globalSession);
			servletContext.setAttribute("global_session_age", globalSessionAge);
		}
		
		return globalSession;
	}

	@Override
	public List<ScoreDto> queryRemoteScores(String sessionId, int schoolYear,
			int term, boolean onlyMax) {
		MapMaker maker = MapMaker.instance("para", 0+"").param("term", term+"").param("year", (schoolYear-1980)+"");
		if(onlyMax){
			maker.param("maxStatus", 1+"");
		}
		FetchResponse fetchResponse = URLFetcher.fetchURLByPost(queryRemoteScoresURL, sessionId, maker.toMap());

		
		if(!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> list = Extractor.$("table[class=datalist] > tr",fetchResponse.getResponseBody());
		List<ScoreDto> scores = new ArrayList<>();
		for(int i=1;i<list.size();i++){
			List<DomElement> scoreTemp = list.get(i).children("td");
			ScoreDto score = new ScoreDto(scoreTemp.get(0).getText().trim(),
					scoreTemp.get(1).getText().trim(),
					scoreTemp.get(2).getText().trim(),
					scoreTemp.get(3).getText().trim(),
					scoreTemp.get(4).getText().trim(),
					scoreTemp.get(5).getText().trim(),
					scoreTemp.get(6).getText().trim(),
					scoreTemp.get(7).getText().trim(),
					scoreTemp.get(8).getText().trim(),
					scoreTemp.get(9).getText().trim(),
					scoreTemp.get(10).getText().trim(),
					scoreTemp.get(11).getText().trim(),
					scoreTemp.get(12).getText().trim(),
					scoreTemp.get(13).getText().trim(),
					scoreTemp.get(14).getText().trim(),
					scoreTemp.get(15).getText().trim());
			scores.add(score);
		}
		return scores;
	}

	@Override
	public List<ArticleDto> queryRemoteNotes(int pagingPage) {
		
		if(pagingPage<1){
			pagingPage = 1;
		}
		
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryNoteURL+"&pagingPage="+pagingPage, null);
		
		List<DomElement> list = Extractor.$("ul[class=articleList] div", fetchResponse.getResponseBody());
		
		List<ArticleDto> articleDtos = new ArrayList<>();

        for (DomElement domElement : list) {
            ArticleDto articleDto = new ArticleDto();

            DomElement aDom = domElement.children("a").get(0);
            articleDto.setHref("http://202.199.128.21/jwzx/" + aDom.attr("href"));
            articleDto.setTitle(aDom.getText().trim());

            //System.err.println("strong:"+aDom.find("strong").size());

            if (aDom.find("strong").size() != 0) {
                articleDto.setImportant(true);
            }

            DomElement spanDom = domElement.find("span").get(0);
            articleDto.setReleaseDate(spanDom.getText().trim());

            articleDtos.add(articleDto);

            //System.err.println(articleDto.toJSON());
        }
		
		
		return articleDtos;
	}

	@Override
	public ArticleDto queryRemoteNote(ArticleDto articleDto) {
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(articleDto.getHref(), null);
		
		DomElement bodyElement = Extractor.$("div[class=body]", fetchResponse.getResponseBody()).get(0);
		
		articleDto.setContent(bodyElement.getHtml());
		
		//System.err.println(articleDto.getContent());
		
		return articleDto;
	}

	@Override
	public CourseRecordDto queryRemoteCourseRecord(String sessionId, String remoteId) {
		//epid=382117465
		FetchResponse fetchResponse = URLFetcher.fetchURLByGet(queryCourseRecord+"epid="+remoteId, sessionId);
		if(fetchResponse.getStatusCode()!=200||!Extractor.$("table[class=error_top]",fetchResponse.getResponseBody()).isEmpty()){
			return null;
		}
		List<DomElement> domElements = Extractor.$("table[class=form] tr", fetchResponse.getResponseBody());
		if(domElements==null||domElements.size()==0){
			return null;
		}
		
		CourseRecordDto courseRecordDto = new CourseRecordDto();
		Iterator<DomElement> iterator = domElements.iterator();
		
		//aliasName and courseProperty
		DomElement domElement = iterator.next();
		List<DomElement> temp = domElement.find("td");
		courseRecordDto.setCourseAliasName(temp.get(0).getText().trim());
		courseRecordDto.setCourseProperty(temp.get(1).getText().trim());
		
		//course attr and exam type
		domElement = iterator.next();
		temp = domElement.find("td");
		courseRecordDto.setCourseAttr(temp.get(0).getText().trim());
		courseRecordDto.setExamType(temp.get(1).getText().trim());
		
		//course capacity and real capacity
		domElement = iterator.next();
		temp = domElement.find("td");
		courseRecordDto.setCourseCapacity(Integer.valueOf(temp.get(0).getText().trim()));
		courseRecordDto.setRealCapacity(Integer.valueOf(temp.get(1).getText().trim()));
		
		//teacher info
		domElement = iterator.next();
		temp = domElement.find("td");
		courseRecordDto.setTeacherInfo(temp.get(0).getText().trim());
		
		//study time
		domElement = iterator.next();
		temp = domElement.find("td");
		courseRecordDto.setStudyTime(Double.valueOf(temp.get(0).getText().trim()));

		//classes
		domElement = iterator.next();
		temp = domElement.find("td");
		courseRecordDto.setClasses(Arrays.asList(temp.get(0).getText().trim().split("\\s+")));
		
		return courseRecordDto;
				
	}

	

}




