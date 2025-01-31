package bert.data.proj;

import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import bert.data.FileProvider;
import bert.data.ProjectProvider;
import bert.data.ProjectSerializer;
import bert.data.proj.exceptions.DuplicateAuditException;
import bert.data.proj.exceptions.InvalidBuildingNameException;
import bert.data.proj.exceptions.InvalidProjectNameException;
import bert.utility.Cleaner;
import bert.utility.DateUtil;

/**
 * @author afiol-mahon
 */
public class Project {
    private String projectName;
    private String contactName;
    private String contactNumber;
    private String creationDate;
    private String modifiedDate;
    private List<BertUnit> bertList;
    private HashMap<String, Building> buildingList;
    private List<RoomAudit> auditList;

    public Project(String name, List<BertUnit> bertList, HashMap<String, Building> buildingList, List<RoomAudit> auditList) throws InvalidProjectNameException {
        this.projectName = name;
        this.creationDate = DateUtil.getDate();
        this.modifiedDate = creationDate;
        this.bertList = bertList;
        this.buildingList = buildingList;
        this.auditList = auditList;
    }

    public boolean isAudit() {
        return bertList.size() == 0;
    }

    public void convertToInstall() {
        for (RoomAudit roomAudit : auditList) {
            bertList.addAll(roomAudit.createBerts());
            auditList.remove(roomAudit);
        }
    }

    public void addAudit(RoomAudit newAudit) throws DuplicateAuditException {
        boolean doesExist = false;
        for (RoomAudit r : auditList) {
            if (r.getRoomID().equals(newAudit.getRoomID()) && r.getBuildingID().equals(newAudit.getBuildingID())) {
                //FIXME: duplicate exeption always throwns
                // throw new DuplicateAuditException();
                doesExist = true;
            }
        }
        if (!doesExist) {
            auditList.add(newAudit);
        }
    }

    public List<RoomAudit> getAuditList() {
        return auditList;
    }

    public RoomAudit getAuditForRoomAndBuilding(String roomID, String buildingID) {
        for (RoomAudit audit : getAuditList()) {
            if (audit.getBuildingID().equals(buildingID) && audit.getRoomID().equals(roomID)) {
                return audit;
            }
        }
        return null;
    }

    public List<String> getRoomNamesInBuilding(String buildingID) {
        List<String> roomList = new ArrayList<>();
        for (BertUnit b : getBerts()) {
            String nextRoom = b.getRoomID();
            if (b.getBuildingID().equals(buildingID) && !roomList.contains(nextRoom)) {
                roomList.add(nextRoom);
            }
        }
        for (RoomAudit audit : getAuditList()) {
            String auditID = audit.getRoomID();
            if (audit.getBuildingID().equals(buildingID) && !roomList.contains(auditID)) {
                roomList.add(auditID);
            }
        }
        return roomList;
    }

    public List<String> getBuildingNames() {
        return new ArrayList<>(buildingList.keySet());
    }

    public List<BertUnit> getBerts() {
        List<BertUnit> output = new ArrayList<>();
        for (BertUnit b : bertList) {
            output.add(b);
        }
        return output;
    }

    public int getBertCount() {
        int bertCount = 0;
        for (String buildingID : buildingList.keySet()) {
            bertCount += getBertCountForBuilding(buildingID);
        }
        return bertCount;
    }

    public int getBertCountForBuilding(String buildingID) {
        int bertCount = 0;
        for (RoomAudit audit : auditList) {
            if (audit.getBuildingID().equals(buildingID)) {
                bertCount += audit.totalBerts();
            }
        }
        for (BertUnit b : bertList) {
            if (b.getBuildingID().equals(buildingID)) {
                bertCount++;
            }
        }
        return bertCount;
    }

    public List<BertUnit> getBertsByRoom(String buildingID, String roomID) {
        List<BertUnit> returnList = new ArrayList<>();
        for (BertUnit b : getBerts()) {
            if (b.getBuildingID().equals(buildingID) && b.getRoomID().equals(roomID)) {
                returnList.add(b);
            }
        }
        return returnList;
    }

    public List<BertUnit> getBertsByBuilding(String buildingID) {
        List<BertUnit> returnList = new ArrayList<>();
        for (BertUnit b : getBerts()) {
            if (b.getBuildingID().equals(buildingID)) {
                returnList.add(b);
            }
        }
        return returnList;
    }

    public List<BertUnit> getBertsByCategory(String buildingID, String categoryID) {
        List<BertUnit> returnList = new ArrayList<>();
        for (BertUnit b : getBerts()) {
            if (b.getBuildingID().equals(buildingID) && b.getCategoryID().equals(categoryID)) {
                returnList.add(b);
            }
        }
        return returnList;
    }

    public int getBertCountForCategory(String buildingID, String categoryID) {
        int bertCount = 0;
        for (RoomAudit roomAudit : auditList) {
            Log.d("CATEGORY BERT COUNTER", "building ID: " + buildingID);

            if (roomAudit.getBuildingID().equals(buildingID)) {
                Log.d("CATEGORY BERT COUNTER", "bertCount addition: " + roomAudit.getCategoryCount(categoryID));
                bertCount += roomAudit.getCategoryCount(categoryID);
            }
        }
        for (BertUnit b : bertList) {
            if (b.getBuildingID().equals(buildingID) && b.getCategoryID().equals(categoryID)) {
                bertCount++;
            }
        }
        return bertCount;
    }

    public void removeCategoryInBuilding(String buildingID, String categoryID) {
        for (RoomAudit roomAudit : auditList) {
            if (roomAudit.getBuildingID().equals(buildingID)) {
                roomAudit.removeCategory(categoryID);
            }
        }
        for (BertUnit b : bertList) {
            if (b.getBuildingID().equals(buildingID) && b.getCategoryID().equals(categoryID)) {
                deleteBert(b);
            }
        }
    }

    public int getRoomCount() {
        int roomCount = 0;
        for (String buildingID : buildingList.keySet()) {
            roomCount += getRoomCountForBuilding(buildingID);
        }
        return roomCount;
    }

    public int getRoomCountForBuilding(String buildingID) {
        int roomCount = 0;
        List<String> roomList = new ArrayList<>();
        for (BertUnit b : getBerts()) {
            if (buildingID.equals(b.getBuildingID()) && !roomList.contains(b.getRoomID())) {
                roomList.add(b.getRoomID());
            }
        }
        for (RoomAudit roomAudit : auditList) {
            if (roomAudit.getBuildingID().equals(buildingID)) {
                roomCount++;
            }
        }
        return roomCount + roomList.size();
    }

    public int getRoomCompletedCountForBuilding(String buildingID) {
        int roomsCompleted = 0;
        for (String roomID : getRoomNamesInBuilding(buildingID)) {
            boolean isAllInstalledSoFar = true;
            for (BertUnit b : getBertsByRoom(buildingID, roomID)) {
                if (!b.isInstalled()) {
                    isAllInstalledSoFar = false;
                }
            }
            if (isAllInstalledSoFar) {
                roomsCompleted++;
            }
        }
        return roomsCompleted;
    }

    public int getRoomCompletedCount() {
        int roomsCompleted = 0;
        for (String buildingID : getBuildingNames()) {
            roomsCompleted += getRoomCompletedCountForBuilding(buildingID);
        }
        return roomsCompleted;
    }

    public int getBertCompletedCountForBuilding(String buildingID) {
        int bertsCompletedCount = 0;
        for (BertUnit b : getBerts()) {
            if (b.getBuildingID().equals(buildingID)) {
                bertsCompletedCount += (b.isInstalled() ? 1 : 0);
            }
        }
        return bertsCompletedCount;
    }

    public int getBertCompletedCount() {
        int bertsCompletedCount = 0;
        for (BertUnit b : getBerts()) {
            bertsCompletedCount += (b.isInstalled() ? 1 : 0);
        }
        return bertsCompletedCount;
    }

    public int getBuildingCount() {
        return buildingList.size();
    }

    public void addBert(BertUnit bert) {
        bertList.add(bert);
    }

    public void deleteBert(BertUnit bert) {
        bertList.remove(bert);
    }

    public Building getBuilding(String buildingID) {
        return buildingList.get(buildingID);
    }

    public void addBuilding(String buildingID, Building building) throws InvalidBuildingNameException {
        if (!buildingList.containsKey(buildingID) && Cleaner.isValid(buildingID)) {
            buildingList.put(buildingID, building);
        } else {
            throw new InvalidBuildingNameException();
        }
    }

    public void renameBuilding(String buildingID, String newBuildingID) throws InvalidBuildingNameException {
        Building building = buildingList.get(buildingID);
        addBuilding(newBuildingID, building);
        buildingList.remove(buildingID);
    }

    public void deleteBuilding(String buildingID) {
        for (BertUnit b : getBertsByBuilding(buildingID)) {
            deleteBert(b);
            deleteBert(b);
        }
        buildingList.remove(buildingID);
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String newProjectName) throws InvalidProjectNameException {
        if (!newProjectName.equals(this.projectName)){
            Cleaner.cleanProjectName(newProjectName);
            if (ProjectProvider.getInstance().projectNameCheck(newProjectName)) {
                String oldProjectName = this.projectName;
                this.projectName = newProjectName;
                ProjectProvider.getInstance().deleteProject(oldProjectName);
            } else {
                throw new InvalidProjectNameException();
            }
        }
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String newContactName) {
        Cleaner.clean(newContactName);
        if (Cleaner.isValid(newContactName)) {
            this.contactName = newContactName;
        }
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String newContactNumber) {
        Cleaner.clean(newContactNumber);
        if (Cleaner.isValid(newContactNumber)) {
            this.contactNumber = newContactNumber;
        }
    }

    public String getCreationDate() {
        return this.creationDate;
    }

    public void setCreationDate(String newCreationDate) {
        this.creationDate = newCreationDate;
    }

    public String getModifiedDate() {
        return this.modifiedDate;
    }

    public void setModifiedDate(String newModifiedDate) {
        this.modifiedDate = newModifiedDate;
    }

    /*
    Loading and Saving
     */

    public File getProjectFile() throws IOException {
        return new File(FileProvider.getProjectDirectory(), this.getProjectName() + ".xml");
    }

    public File getProjectFileNoSpaces() throws IOException{
        return new File(getProjectFile().getAbsolutePath().replace(" ", "%20"));
    }

    static public File getProjectFile(String projectID) throws IOException {
        return new File(FileProvider.getProjectDirectory(), projectID + ".xml");
    }

    static public File getProjectFileNoSpaces(String projectID) throws IOException{
        return new File(getProjectFile(projectID).getAbsolutePath().replace(" ", "%20"));
    }

    public void save() {
        Log.d("ProjectSaver", "Saving Project: " + getProjectName() + " to XML file");
        Document d = ProjectSerializer.exportToXML(this);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            File outputFile = getProjectFile();
            Result result = new StreamResult(outputFile);
            Source source = new DOMSource(d);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();//TODO should these be handled better?
        }
    }

    public static Project loadProject(InputStream stream) throws InvalidProjectNameException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document d = builder.parse(stream);
            return ProjectSerializer.getProjectFromDocument(d);
        } catch (ParserConfigurationException e){
            e.printStackTrace();
            return null;
        } catch ( IOException e){
            e.printStackTrace();
            return null;
        } catch (SAXException e){
            e.printStackTrace();
            return null;
        }

    }

    public static Project loadProject(File file) throws InvalidProjectNameException {
        Log.d("ProjectLoader", "Loading document: " + file.getName());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document d = builder.parse(file);
            return ProjectSerializer.getProjectFromDocument(d);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}