package bert.ui.roomList.deviceList.deviceEditor;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import bert.data.ProjectProvider;
import bert.ui.common.BertAlert;
import bert.ui.common.NoSelectionFragment;
import bert.ui.R;
import bert.ui.common.ProjectChildEditorFragment;

public class DeviceListFragment extends ProjectChildEditorFragment {
    public static final String ARG_LOCATION = "LOCATION";
    public static final String ARG_BUILDING = "BUILDING_ID";
    public static final String ARG_PROJECT = "PROJECT_ID";

    public static final String ADD_BUILDING_STRING = "+ Add Bert";

    private String projectID;
    private String buildingID;
    private String location;

    private DeviceDetailListGVA deviceTableAdapter;
    private ListView locationListView;

    public static DeviceListFragment newInstance(String projectID, String buildingID, String location) {
        DeviceListFragment fragment = new DeviceListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT, projectID);
        args.putString(ARG_BUILDING, buildingID);
        args.putString(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }

    public DeviceListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectID = getArguments().getString(ARG_PROJECT);
            buildingID = getArguments().getString(ARG_BUILDING);
            location = getArguments().getString(ARG_LOCATION);
        }
        loadFragment(NoSelectionFragment.newInstance("Select or Create a Bert"));
    }

    @Override
    public void onResume() {
        super.onResume();
        project = ProjectProvider.getInstance().getProject(projectID);

        locationListView = (ListView) getView().findViewById(R.id.bertList);

        deviceTableAdapter = new DeviceDetailListGVA(getActivity(), this, project.getBertsByRoom(buildingID, location));
        locationListView = (ListView) getView().findViewById(R.id.bertList);
        locationListView.setAdapter(deviceTableAdapter);
        locationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == project.getBertsByRoom(buildingID, location).size()) {
                    addDevice();
                } else {
                    loadDeviceAtPosition(position);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.device_list_fragment, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void openNewestDeviceDetailFragment() {
        loadDeviceAtPosition(deviceTableAdapter.getCount() - 2);
    }

    public void loadDeviceAtPosition(int position) {
        System.out.println("editing device");
        DeviceDetailFragment fragment = DeviceDetailFragment.newInstance(projectID, buildingID, location, position);
        loadFragment(fragment);
    }

    public void addDevice() {
        System.out.println("adding device");
        DeviceAddFragment fragment = DeviceAddFragment.newInstance(projectID, buildingID, location);
        loadFragment(fragment);
    }

    private void loadFragment(Fragment frag) {
        FragmentTransaction t = getFragmentManager().beginTransaction();
        t.replace(R.id.bertDeviceDetailViewFrame, frag);
        t.addToBackStack(null);
        t.commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (deviceTableAdapter.berts.size() == 0) {
            BertAlert.show(getActivity(), "This room has no berts, it will be automatically deleted");
        }
    }
}