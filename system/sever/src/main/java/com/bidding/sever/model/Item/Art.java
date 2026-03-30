public class Art extends Item {

    private String artist;
    private String medium;
    private int yearCreated;
    private String dimensions;
    
    public Art() {
        super();
    }

    public Art(String id, String name, String description, double startingPrice, List<String> images, User seller, ItemCondition condition, String artist, String medium, int yearCreated, String dimensions) {
        super(id, name, description, startingPrice, images, seller, condition);
        this.artist = artist;
        this.medium = medium;
        this.yearCreated = yearCreated;
        this.dimensions = dimensions;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    

    @Override
    public String getCategory() {
        return "Art";
    }
    
    @Override
    public Map<String, String> getSpecifications() {
        Map<String, String> specifications = super.getSpecifications();
        specifications.put("Artist", artist);
        specifications.put("Medium", medium);
        specifications.put("Year Created", String.valueOf(yearCreated));
        specifications.put("Dimensions", dimensions);
        return specifications;
    }
}
