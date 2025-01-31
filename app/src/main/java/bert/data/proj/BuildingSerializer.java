package bert.data.proj;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;

/**
 * Created by afiol-mahon on 5/13/15.
 */
public class BuildingSerializer {

    public static final String TAG_BUILDING = "Building";
    public static final String ATTR_NAME = "Name";
    public static final String ATTR_START_TIME = "StartTime";
    public static final String ATTR_END_TIME = "EndTime";

    public static Building getBuildingFromElement(Element e) {
        Time startTime;
        try {
            startTime = new Time(Integer.parseInt(e.getAttribute(ATTR_START_TIME)));
        } catch ( NumberFormatException ex) {
            ex.printStackTrace();
            startTime = new Time(9, 0);
        }

        Time endTime;
        try {
            endTime = new Time(Integer.parseInt(e.getAttribute(ATTR_END_TIME)));
        } catch ( NumberFormatException e2) {
            e2.printStackTrace();
            endTime = new Time(20, 0);
        }

        HashMap<String, Category> categories = new HashMap<>();
        NodeList categoryElements = e.getElementsByTagName(CategorySerializer.TAG_CATEGORY);
        for (int i = 0; i < categoryElements.getLength(); i++) {
            Element categoryElement = (Element) categoryElements.item(i);
            String categoryID = categoryElement.getAttribute(CategorySerializer.ATTR_ID);
            Category cat = CategorySerializer.getCategoryFromElement(categoryElement);
            categories.put(categoryID, cat);
        }
        return new Building(startTime, endTime, categories);
    }

    public static String getBuildingNameFromElement(Element e) {
        return e.getAttribute(ATTR_NAME);
    }

    public static Element getElementFromBuilding(String buildingID, Building b, Document d) {
        Element e = d.createElement(TAG_BUILDING);
        e.setAttribute(ATTR_NAME, buildingID);
        e.setAttribute(ATTR_START_TIME, Integer.toString(b.getStartTime().getRaw()));
        e.setAttribute(ATTR_END_TIME, Integer.toString(b.getEndTime().getRaw()));

        for(String categoryID : b.getCategoryNames()) {
            e.appendChild(CategorySerializer.getElementFromCategory(categoryID, b.getCategory(categoryID), d));
        }

        return e;
    }
}
