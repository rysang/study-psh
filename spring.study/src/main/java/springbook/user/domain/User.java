package springbook.user.domain;

public class User {

	String id;
	String name;
	String password;
	private Level level;
	private int login;
	private int recommend;
	
	public User(String id, String name, String password, Level level, int login, int recommend) {
		this.id = id;
		this.name = name;
		this.password = password;
		this.level = level;
		this.login = login;
		this.recommend = recommend;
	}
	
	public User() {
		// TODO Auto-generated constructor stub
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public void setLogin(int login) {
		this.login = login;
	}

	public int getLogin() {
		return login;
	}

	public void setRecommend(int recommend) {
		this.recommend = recommend;
	}

	public int getRecommend() {
		return recommend;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public Level getLevel() {
		return level;
	}
	
	public void upgradeLevel() {
		Level nextLevel = this.level.nextLevel();
		if(nextLevel == null) {
			throw new IllegalAccessError(this.level + "¾ÈµÅ ");
		} else {
			this.level = nextLevel;
		}
	}
	
}
