package study.dto;

public class User {

	public String id;
	private String name;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		System.out.println("id:"+id);
		this.id = id;
	}
	
}
