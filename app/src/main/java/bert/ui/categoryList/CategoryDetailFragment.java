package bert.ui.categoryList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import bert.data.ProjectProvider;
import bert.data.proj.Building;
import bert.data.proj.Category;
import bert.data.proj.exceptions.InvalidCategoryNameException;
import bert.data.proj.exceptions.UnableToDeleteException;
import bert.ui.common.BertAlert;
import bert.ui.R;
import bert.ui.common.ProjectChildEditorFragment;

public class CategoryDetailFragment extends ProjectChildEditorFragment {
    private static final String ARG_PROJECT_ID = "PROJECT_ID";
    private static final String ARG_BUILDING_ID = "BUILDING_ID";
    private static final String ARG_CATEGORY_ID = "CATEGORY_ID";

    private static final String UNDEFINED_LOAD_STRING = "Undefined";

    private String projectID;
    private String buildingID;
    private String categoryID;

    private CategoryListActivity activity;

    private Building building;
    private Category category;

    private EditText categoryNameEditText;
    private EditText estimatedLoadEditText;
    private Spinner bertTypeSpinner;
    private ArrayAdapter<String> bertTypeSpinnerAdapter;
    private TextView bertCountDisplay;

    private TextView multipleDevicesIndicator;
    private Button saveButton;
    private Button deleteButton;

    private OnFragmentInteractionListener mListener;

    public static CategoryDetailFragment newInstance(String projectID, String buildingID, String categoryID) {
        CategoryDetailFragment fragment = new CategoryDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectID);
        args.putString(ARG_BUILDING_ID, buildingID);
        args.putString(ARG_CATEGORY_ID, categoryID);
        fragment.setArguments(args);
        return fragment;
    }

    public CategoryDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectID = getArguments().getString(ARG_PROJECT_ID);
            categoryID = getArguments().getString(ARG_CATEGORY_ID);
            buildingID = getArguments().getString(ARG_BUILDING_ID);

            project = ProjectProvider.getInstance().getProject(projectID);
            building = project.getBuilding(buildingID);
            category = building.getCategory(categoryID);
        }
        activity = (CategoryListActivity) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();

        categoryNameEditText = (EditText) getView().findViewById(R.id.category_name_edit_text);
        categoryNameEditText.setText(categoryID);
        categoryNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(textView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return false;
            }
        });

        estimatedLoadEditText = (EditText) getView().findViewById(R.id.estimated_load_edit_text);
        estimatedLoadEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(textView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return false;
            }
        });

        estimatedLoadEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && estimatedLoadEditText.getText().toString().contentEquals(UNDEFINED_LOAD_STRING)) {
                    estimatedLoadEditText.setText("");
                }
            }
        });

        if (category.getEstimatedLoad() != Category.UNSET_ESTIMATED_LOAD) {
            estimatedLoadEditText.setText(String.valueOf(category.getEstimatedLoad()));
        } else {
            estimatedLoadEditText.setText(UNDEFINED_LOAD_STRING);
        }

        bertTypeSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, Category.bertTypes);
        bertTypeSpinner = (Spinner) getView().findViewById(R.id.bert_type_spinner);
        bertTypeSpinner.setAdapter(bertTypeSpinnerAdapter);
        bertTypeSpinner.setSelection(category.getBertTypeID());

        multipleDevicesIndicator = (TextView) getView().findViewById(R.id.multiple_device_text_view);
        multipleDevicesIndicator.setText(String.valueOf(category.doesRequireExtensionCord()));

        bertCountDisplay = (TextView) getView().findViewById(R.id.bert_count_label);
        bertCountDisplay.setText(String.valueOf(project.getBertCountForCategory(buildingID, categoryID)));

        saveButton = (Button) getView().findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });

        deleteButton = (Button) getView().findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askToDeleteCategory();
            }
        });

        activity.onResume();
    }

    private void askToDeleteCategory () {
        int numberOfBuildingsWithCategory = 0;
        for (String buildingID : project.getBuildingNames()) {
            if (project.getBuilding(buildingID).getCategory(categoryID) != null) {
                numberOfBuildingsWithCategory++;
            }
        }
        if (numberOfBuildingsWithCategory > 1) {
            BertAlert.show(getActivity(),
                    "Delete In All Buildings?",
                    "There are " + (numberOfBuildingsWithCategory - 1) + " other buildings that have this category. Delete in all buildings?",
                    "No, only delete in " + buildingID, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            askToDeleteCategoriesInAllBuildinds(false);
                        }
                    }, "Yes, delete in all buildings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            askToDeleteCategoriesInAllBuildinds(true);
                        }
                    });
        }
    }



    private void askToDeleteCategoriesInAllBuildinds(final boolean deleteInAllBuildings) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("Are you sure you want to delete this category?");
        if (project.getBertCountForCategory(buildingID, categoryID) > 0) {
            int totalBertsInCategory;
            if (deleteInAllBuildings) {
                totalBertsInCategory = 0;
                for (String buildingID : project.getBuildingNames()){
                    totalBertsInCategory += project.getBertCountForCategory(buildingID, categoryID);
                }
            } else {
                totalBertsInCategory = project.getBertCountForCategory(buildingID, categoryID);
            }
            alert.setMessage("All " + String.valueOf(String.valueOf(totalBertsInCategory) + " berts in this category will be deleted as well."));
        }
        alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteCategoryInAllBuildings(deleteInAllBuildings);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        alert.create().show();
    }

    private void deleteCategoryInAllBuildings(boolean inAllBuildings) {
        try {
            if (inAllBuildings) {
                for (String buildingID : project.getBuildingNames()) {
                    project.getBuilding(buildingID).deleteCategory(project, buildingID, categoryID);
                }
            } else {
                building.deleteCategory(project, buildingID, categoryID);
            }

            project.save();
            activity.onResume();
            activity.openNoSelectionFragment();
        } catch (UnableToDeleteException e) {
            int bertsRemaining = project.getBertsByCategory(buildingID, categoryID).size();
            String text = (bertsRemaining == 1) ? " Bert uses this category." : " Berts use this category.";
            BertAlert.show(getActivity(), "Unable to delete category: " + bertsRemaining + text);
        }
    }
    private void saveChanges() {
        try {
            String newCategoryID = categoryNameEditText.getText().toString();
            building.renameCategory(categoryID, newCategoryID);
            categoryID = newCategoryID;
            try {
                int estimatedLoad = Integer.valueOf(estimatedLoadEditText.getText().toString());
                category.setEstimatedLoad(estimatedLoad);
            } catch (NumberFormatException e) {
                category.setEstimatedLoad(Category.UNSET_ESTIMATED_LOAD);
                if (!estimatedLoadEditText.hasFocus()) {
                    estimatedLoadEditText.setText(UNDEFINED_LOAD_STRING);
                }
            }
            category.setBertTypeID(bertTypeSpinner.getSelectedItemPosition());
            project.save();
            activity.createCategoryListView();
        } catch (InvalidCategoryNameException e) {
            e.printStackTrace();
            BertAlert.show(getActivity(), "Invalid New Category Name");
            categoryNameEditText.setText(categoryID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.category_detail_fragment, container, false);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }
}
