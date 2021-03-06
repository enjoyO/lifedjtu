package com.lifedjtu.jw.pojos.dto;

import com.lifedjtu.jw.pojos.EntityObject;

public class ExamDto extends EntityObject{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3594352363523619003L;
	private String courseAliasName;
	private String courseName;
	private String examDate;
	private String roomName;
	private String courseProperty;
	
	public ExamDto(String courseAliasName, String courseName, String examDate,
			String roomName, String courseProperty) {
		super();
		this.courseAliasName = courseAliasName;
		this.courseName = courseName;
		this.examDate = examDate;
		this.roomName = roomName;
		this.courseProperty = courseProperty;
	}
	
	

	public String getCourseAliasName() {
		return courseAliasName;
	}
	public void setCourseAliasName(String courseAliasName) {
		this.courseAliasName = courseAliasName;
	}
	public String getCourseName() {
		return courseName;
	}
	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	public String getExamDate() {
		return examDate;
	}
	public void setExamDate(String examDate) {
		this.examDate = examDate;
	}
	public String getRoomName() {
		return roomName;
	}
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	public String getCourseProperty() {
		return courseProperty;
	}
	public void setCourseProperty(String courseProperty) {
		this.courseProperty = courseProperty;
	}
	
	@Override
	public String toString() {
		return "Exam [courseAliasName=" + courseAliasName + ", courseName="
				+ courseName + ", examDate=" + examDate + ", roomName="
				+ roomName + ", courseProperty=" + courseProperty + "]\n";
	}
	
}
