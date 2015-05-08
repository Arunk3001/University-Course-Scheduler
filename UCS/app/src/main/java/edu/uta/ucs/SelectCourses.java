package edu.uta.ucs;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Holds all course info for an entire semester for AutoComplete and filtering of courses for validity
 */
class SemesterInfo{


    private int semesterNumber;
    private String semesterName;
    private ArrayList<DepartmentInfo> departmentArrayList;


    public static ArrayList<SemesterInfo> SemesterInfoFactory(JSONObject SemesterRaw) throws JSONException {
        /*if(SemesterRaw.getBoolean("Success"))
            return null;*/
        JSONArray semestersArray = SemesterRaw.getJSONArray("Semesters");
        ArrayList<SemesterInfo> results = new ArrayList<>(semestersArray.length());

        for(int index = semestersArray.length(); index != 0;index--){
            SemesterInfo parsedSemester = new SemesterInfo(semestersArray.getJSONObject( index-1 ));
            results.add(parsedSemester);
        }
        Collections.reverse(results);

        Log.i("SemesterInfo","Built Semester Info Arraylist");
        return results;
    }

    public static String getSEMESTER_INFO() {
        return "SEMESTER_INFO";
    }

    public String getSemesterName() {
        return semesterName;
    }

    @SuppressWarnings("unused")
    private class SemesterInfoFactoryASYNC extends AsyncTask<JSONObject, Void, ArrayList>{

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected ArrayList doInBackground(JSONObject... params) {
            return null;
        }
    }

    /**
     * Constructor
     *
     * @param semesterInfoRaw JSON Object must have the following keys present:
     *                   <ul>
     *                   <li>"Success" - boolean, represents good server response
     *                   <li>"SemesterNumber" - integer, UTA semster number. EX: 2155 for Summer 2015, 2158 for Spring 2015
     *                   <li>"Departments" - JSONArray, Departments information. See DepartmentInfo constructor for requirements
     *                   <ul/>
     * @throws JSONException
     */
    SemesterInfo(JSONObject semesterInfoRaw) throws JSONException {
        this.semesterNumber = semesterInfoRaw.getInt("SemesterNumber");
        this.semesterName = semesterInfoRaw.getString("SemesterName");
        Log.i("Semester Number", ((Integer) getSemesterNumber()).toString());
        JSONArray departmentJSONArrayRaw = semesterInfoRaw.getJSONArray("Departments");
        this.departmentArrayList = new ArrayList<>(departmentJSONArrayRaw.length());

        for(int index = departmentJSONArrayRaw.length(); index != 0;index--){
            this.getDepartmentArrayList().add(new DepartmentInfo(departmentJSONArrayRaw.getJSONObject(index - 1)));
            }

    }

    public JSONObject toJSON() throws JSONException {

        ArrayList<JSONObject> departmentInfoArray = new ArrayList<>(getDepartmentArrayList().size());
        for(DepartmentInfo departmentInfo : getDepartmentArrayList()){
            departmentInfoArray.add(departmentInfo.toJSON());
        }
        JSONArray departmentInfoJSONArray = new JSONArray(departmentInfoArray);

        JSONObject semesterInfoJSON = new JSONObject();
        semesterInfoJSON.put("SemesterNumber", ((Integer) this.getSemesterNumber()).toString());
        semesterInfoJSON.put("SemesterName", this.semesterName);
        semesterInfoJSON.put("Departments", departmentInfoJSONArray);
        return semesterInfoJSON;
    }

    public static void saveSemestersToFile(ArrayList <SemesterInfo> semestersToSave, Context context){
        SharedPreferences.Editor savedSemesters = context.getSharedPreferences(SemesterInfo.getSEMESTER_INFO(), Context.MODE_PRIVATE).edit();
        savedSemesters.clear();
        for (SemesterInfo semesterInfo : semestersToSave){
            try {
                Log.i("Semester Info Save", "Saving Semester Number: " + semesterInfo.getSemesterNumber());
                String semesterInfoJSON = semesterInfo.toJSON().toString();
                savedSemesters.putString( "SEMESTER_INFO_" + semesterInfo.getSemesterNumber() + "", semesterInfoJSON);//SemesterInfo.getSEMESTER_INFO() + "_" +
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        savedSemesters.apply();
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public ArrayList<DepartmentInfo> getDepartmentArrayList() {
        return departmentArrayList;
    }

    /**
     * Holds a departments' ID, Title, and an arraylist of CourseInfo to hold course information for all courses in that department.
     */
    class DepartmentInfo {

        private int departmentID;
        private String departmentAcronym;
        private String departmentTitle;
        private ArrayList<CourseInfo> courses;

        /**
         * Constructor
         *
         * @param departmentInfoRaw JSON Object must have the following keys present:
         *                   <ul>
         *                   <li>"ID" - string, abbreviated department title as used in MyMav.
         *                          EX: "ENGL" for "English" department
         *                              "ECED" for "Early Childhood Education" department
         *                   <li>"Title" - string, full name of department. EX: "English" or "Early Childhood Education"
         *                   <li>"Courses" - JSONArray, Course Information. See CourseInfo constructor for requirements
         *                   <ul/>
         * @throws JSONException
         */
        public DepartmentInfo(JSONObject departmentInfoRaw) throws JSONException {
            this.setDepartmentID(departmentInfoRaw.getInt("Id"));
            this.setDepartmentAcronym(departmentInfoRaw.getString("DepartmentAcronym"));
            this.setDepartmentTitle(departmentInfoRaw.getString("DepartmentName"));//departmentInfoRaw.getString("Title");
            JSONArray courseJSONArrayRaw = departmentInfoRaw.getJSONArray("CourseNumbers");
            this.courses = new ArrayList<>(courseJSONArrayRaw.length());

            for(int index = courseJSONArrayRaw.length(); index != 0;index--){
                this.getCourses().add(new CourseInfo(courseJSONArrayRaw.getJSONObject(index - 1), this));
            }

            Log.i("Department Details", "New Department Added:"+ getDepartmentID() + " " + getDepartmentAcronym() + " " + getDepartmentTitle() + " " + getCourses().size());
        }

        @SuppressWarnings("unused")
        public DepartmentInfo(){
            this.setDepartmentID(0);
            this.setDepartmentAcronym(null);
            this.setDepartmentTitle(null);
            this.setCourses(null);
        }

        public JSONObject toJSON() throws JSONException {

            ArrayList<JSONObject> courseInfoArray = new ArrayList<>(courses.size());
            for(CourseInfo courseInfo : courses){
                courseInfoArray.add(courseInfo.toJSON());
            }
            JSONArray courseInfoJSONArray = new JSONArray(courseInfoArray);

            JSONObject departmentInfoJSON = new JSONObject();

            departmentInfoJSON.put("Id", departmentID);
            departmentInfoJSON.put("DepartmentAcronym",departmentAcronym);
            departmentInfoJSON.put("DepartmentName", departmentTitle);
            departmentInfoJSON.put("CourseNumbers", courseInfoJSONArray);

            return departmentInfoJSON;
        }

        public int getDepartmentID() {
            return departmentID;
        }

        public void setDepartmentID(int departmentID) {
            this.departmentID = departmentID;
        }

        public String getDepartmentTitle() {
            return departmentTitle;
        }

        public void setDepartmentTitle(String departmentTitle) {
            this.departmentTitle = departmentTitle;
        }

        public ArrayList<CourseInfo> getCourses() {
            return courses;
        }

        public void setCourses(ArrayList<CourseInfo> courses) {
            this.courses = courses;
        }

        public String getDepartmentAcronym() {
            return departmentAcronym;
        }

        public void setDepartmentAcronym(String departmentAcronym) {
            this.departmentAcronym = departmentAcronym;
        }

        /**
         * Holds a course number for autocomplete as well as the tile for that course number.
         */
        class CourseInfo{

            private int courseNumber;
            private String courseTitle;
            private DepartmentInfo departmentInfo;

            /**
             * Constructor
             * @param courseInfoJSONObject JSON Object must have the following keys present:
             *                   <ul>
             *                   <li>"CourseNumber" - integer, represents the course number in the course info
             *                   <li><t>In an example course, such as "ENGL-1301", this would the the "1301" part
             *                   <li>"CourseTitle" - string, course title. Example: "RHETORIC AND COMPOSITION I" for ENGL-1301
             *                   <ul/>
             * @throws JSONException
             */
            public CourseInfo(JSONObject courseInfoJSONObject, DepartmentInfo departmentInfo) throws JSONException {
                this.courseNumber = courseInfoJSONObject.getInt("CourseNumber");
                this.courseTitle = courseInfoJSONObject.getString("CourseName");
                //Log.i("Course Details", "New Course Added:" + " " + this.courseNumber + " " + this.courseTitle);
                this.departmentInfo = departmentInfo;
            }

            /*public JSONObject toJSONIsolated() throws JSONException {
                JSONObject courseInfo = new JSONObject();
                courseInfo.put("CourseNumber", courseNumber);
                courseInfo.put("CourseTitle", courseTitle);
                courseInfo.put("DepartmentAcronym", getDepartmentAcronym());
                return courseInfo;
            }*/

            public JSONObject toJSON() throws JSONException {
                JSONObject courseInfo = new JSONObject();
                courseInfo.put("CourseNumber", courseNumber);
                courseInfo.put("CourseName", courseTitle);
                return courseInfo;
            }

            public int getCourseNumber() {
                return courseNumber;
            }

            public String getCourseTitle() {
                return courseTitle;
            }

            public DepartmentInfo getDepartmentInfo() {
                return departmentInfo;
            }

        }
    }
}

class DepartmentInfoArrayAdapter extends ArrayAdapter<SemesterInfo.DepartmentInfo> implements Filterable{

    private ArrayList<SemesterInfo.DepartmentInfo> departmentInfoArrayList = new ArrayList<>();
    private ArrayList<SemesterInfo.DepartmentInfo> departmentInfoArrayListAll = new ArrayList<>();
    private Context context;

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public DepartmentInfoArrayAdapter(Context context, int resource, ArrayList<SemesterInfo.DepartmentInfo> objects) {
        super(context, resource, objects);
        this.departmentInfoArrayList = objects;
        this.departmentInfoArrayListAll = objects;
        this.context = context;
    }

    @Override
    public int getCount() {
        return departmentInfoArrayList.size();
    }

    @Override
    public SemesterInfo.DepartmentInfo getItem(int position) {
        return departmentInfoArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.desired_courses_listview, parent, false);
        }

        TextView departmentID = ((TextView) convertView.findViewById(R.id.desiredCourseDepartment));
        TextView departmentNumber = ((TextView) convertView.findViewById(R.id.desiredCourseNumber));
        TextView departmentTitle = ((TextView) convertView.findViewById(R.id.desiredCourseTitle));

        SemesterInfo.DepartmentInfo departmentInfo = departmentInfoArrayList.get(position);

        departmentID.setText(departmentInfo.getDepartmentAcronym());
        departmentNumber.setText("");
        departmentTitle.setText("\t" + departmentInfo.getDepartmentTitle());

        convertView.findViewById(R.id.desiredCourseButton).setVisibility(View.GONE);

        return convertView;
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                departmentInfoArrayList = departmentInfoArrayListAll;
                List<SemesterInfo.DepartmentInfo> results = new ArrayList<>();
                if (constraint != null){
                    for(SemesterInfo.DepartmentInfo departmentInfo : departmentInfoArrayList){

                        if(departmentInfo.getDepartmentAcronym().toUpperCase().contains(" NULL"))
                            continue;
                        if(departmentInfo.getDepartmentTitle().toUpperCase().contains(" NULL"))
                            continue;

                        if(departmentInfo.getDepartmentAcronym()==null)
                            continue;
                        if(departmentInfo.getDepartmentTitle()==null)
                            continue;

                        if(departmentInfo.getDepartmentAcronym().toUpperCase().startsWith(constraint.toString().toUpperCase())){
                            if (!results.contains(departmentInfo))
                                results.add(departmentInfo);
                        }
                        if(departmentInfo.getDepartmentTitle().toUpperCase().contains(constraint.toString().toUpperCase())){
                            if (!results.contains(departmentInfo))
                                results.add(departmentInfo);
                        }

                    }
                }
                filterResults.values = results;
                filterResults.count = results.size();
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked cast")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0){
                    departmentInfoArrayList = (ArrayList<SemesterInfo.DepartmentInfo>) results.values;
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}

class CourseInfoArrayAdapter extends ArrayAdapter<SemesterInfo.DepartmentInfo.CourseInfo> implements Filterable{

    private ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> courseInfoArrayList = new ArrayList<>();
    private ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> courseInfoArrayListAll = new ArrayList<>();
    private Context context;

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public CourseInfoArrayAdapter(Context context, int resource, ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> objects) {
        super(context, resource, objects);
        this.courseInfoArrayList = objects;
        this.courseInfoArrayListAll = objects;
        this.context = context;
    }

    @Override
    public int getCount() {
        return courseInfoArrayList.size();
    }

    @Override
    public SemesterInfo.DepartmentInfo.CourseInfo getItem(int position) {
        return courseInfoArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.desired_courses_listview, parent, false);
        }

        TextView departmentID = ((TextView) convertView.findViewById(R.id.desiredCourseDepartment));
        TextView departmentNumber = ((TextView) convertView.findViewById(R.id.desiredCourseNumber));
        TextView departmentTitle = ((TextView) convertView.findViewById(R.id.desiredCourseTitle));

        SemesterInfo.DepartmentInfo.CourseInfo courseInfo = courseInfoArrayList.get(position);

        //departmentID.setText(courseInfo.getDepartmentInfo().getDepartmentAcronym());
        departmentID.setText("");
        departmentNumber.setText(((Integer) courseInfo.getCourseNumber()).toString());
        departmentTitle.setText("\t" + courseInfo.getCourseTitle());

        convertView.findViewById(R.id.desiredCourseButton).setVisibility(View.GONE);

        return convertView;
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                courseInfoArrayList = courseInfoArrayListAll;
                List<SemesterInfo.DepartmentInfo.CourseInfo> results = new ArrayList<>();
                if (constraint != null){
                    for(SemesterInfo.DepartmentInfo.CourseInfo courseInfo : courseInfoArrayList){
                        if(courseInfo.getCourseTitle().toUpperCase().contains(" NULL"))
                            continue;
                        if(((Integer) courseInfo.getCourseNumber()).toString().contains(constraint.toString())){
                            if (!results.contains(courseInfo))
                                results.add(courseInfo);
                        }
                        if(courseInfo.getCourseTitle().toUpperCase().contains(constraint.toString().toUpperCase())){
                            if (!results.contains(courseInfo))
                                results.add(courseInfo);
                        }
                    }
                }
                filterResults.values = results;
                filterResults.count = results.size();
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked cast")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0){
                    courseInfoArrayList = (ArrayList<SemesterInfo.DepartmentInfo.CourseInfo>) results.values;
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}

class DesiredCoursesArrayAdapter extends ArrayAdapter<SemesterInfo.DepartmentInfo.CourseInfo>{

    ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> courseInfoArrayList;

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     */
    public DesiredCoursesArrayAdapter(Context context, int resource, ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> item ) {
        super(context, resource, item);
        courseInfoArrayList = item;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            view = vi.inflate(R.layout.desired_courses_listview, null);
        }

        final SemesterInfo.DepartmentInfo.CourseInfo courseInfo = getItem(position);

        if (courseInfo != null){
            TextView desiredCourseDepartment = ((TextView) view.findViewById(R.id.desiredCourseDepartment));
            TextView desiredCourseNumber = ((TextView) view.findViewById(R.id.desiredCourseNumber));
            TextView desiredCourseTitle = ((TextView) view.findViewById(R.id.desiredCourseTitle));

            Button desiredCourseRemoveButton = ((Button) view.findViewById(R.id.desiredCourseButton));

            String Department = courseInfo.getDepartmentInfo().getDepartmentAcronym();
            String Number = " - " + ((Integer) courseInfo.getCourseNumber()).toString();
            String Title = "\t" + courseInfo.getCourseTitle();

            if (Department != null){
                desiredCourseDepartment.setText(Department);
            }

            desiredCourseNumber.setText(Number);

            desiredCourseTitle.setText(Title);

            desiredCourseRemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    courseInfoArrayList.remove(courseInfo);
                    notifyDataSetChanged();
                }
            });
        }

        return view;
    }
}


public class SelectCourses extends ActionBarActivity {

    public static final String URL_GET_COURSE_SECTIONS = UserData.getContext().getString(R.string.get_course_section_base);
    public static final String URL_GET_COURSE_SECTIONS_PARAM_SEMESTER = UserData.getContext().getString(R.string.get_course_sections_param_semester);
    public static final String URL_GET_COURSE_SECTIONS_PARAM_DEPARTMENT = UserData.getContext().getString(R.string.get_course_sections_param_department);
    public static final String URL_GET_COURSE_SECTIONS_PARAM_COURSENUMBER = UserData.getContext().getString(R.string.get_course_sections_param_course_number);

    public static final String ACTION_GET_DESIRED_COURSE_SECTIONS ="edu.uta.ucs.intent.action.ACTION_GET_DESIRED_COURSE_SECTIONS";

    public static final String URL_GET_SEMESTER = UserData.getContext().getString(R.string.get_semester_data);
    public static final String ACTION_GET_SEMESTER ="edu.uta.ucs.intent.action.ACTION_GET_SEMESTER";

    private AutoCompleteTextView courseDepartment;
    private AutoCompleteTextView courseNumber;

    private ListView desiredCoursesListView;
    private DesiredCoursesArrayAdapter desiredCoursesArrayAdapter;
    private DepartmentInfoArrayAdapter departmentInfoArrayAdapter;
    private CourseInfoArrayAdapter courseInfoArrayAdapter;

    private Course blockoutTimes = null;
    private ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> desiredCoursesArrayList;

    private Button addCourse;

    private SemesterInfo selectedSemester;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_courses);

        ArrayList<SemesterInfo.DepartmentInfo> departmentInfoArrayList = new ArrayList<>();
        ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> courseInfoArrayList = new ArrayList<>();

        setTitle("Schedule Setup");

        LocalBroadcastManager.getInstance(this).registerReceiver(new DepartmentCoursesReceiver(), new IntentFilter(ACTION_GET_SEMESTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(new DesiredSectionsReceiver(), new IntentFilter(ACTION_GET_DESIRED_COURSE_SECTIONS));

        departmentInfoArrayAdapter = new DepartmentInfoArrayAdapter(this,R.layout.desired_courses_listview, departmentInfoArrayList);
        courseInfoArrayAdapter = new CourseInfoArrayAdapter(this,R.layout.desired_courses_listview, courseInfoArrayList);

        desiredCoursesListView = (ListView) findViewById(R.id.selected_courses_listview);

        courseDepartment = ((AutoCompleteTextView) findViewById(R.id.course_department_edittext));
        courseNumber = ((AutoCompleteTextView) findViewById(R.id.course_number_edittext));

        addCourse = (Button) findViewById(R.id.add_course_button);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                SettingsActivity.startActivity(SelectCourses.this);
                break;
            case R.id.action_logout:
                UserData.logout(SelectCourses.this);
                //signOut();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        courseDepartment.setThreshold(0);
        courseDepartment.setAdapter(departmentInfoArrayAdapter);
        courseDepartment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SemesterInfo.DepartmentInfo departmentInfo = (SemesterInfo.DepartmentInfo) parent.getItemAtPosition(position);
                courseDepartment.setText(departmentInfo.getDepartmentAcronym());
                updateCourseInfoAdapter(departmentInfo.getCourses());
            }
        });
        courseDepartment.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    SemesterInfo.DepartmentInfo departmentInfo = getDepartmentInfo(courseDepartment.getText().toString());
                    if (departmentInfo != null){
                        courseDepartment.setText(departmentInfo.getDepartmentAcronym());
                        updateCourseInfoAdapter(departmentInfo.getCourses());
                    }
                }
            }
        });

        courseNumber.setThreshold(0);
        courseNumber.setAdapter(courseInfoArrayAdapter);
        courseNumber.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SemesterInfo.DepartmentInfo.CourseInfo courseInfo = (SemesterInfo.DepartmentInfo.CourseInfo) parent.getItemAtPosition(position);
                courseNumber.setText(((Integer) courseInfo.getCourseNumber()).toString());
            }
        });

        desiredCoursesArrayList = new ArrayList<>();
        desiredCoursesArrayAdapter = new DesiredCoursesArrayAdapter(SelectCourses.this, R.layout.desired_courses_listview, desiredCoursesArrayList);
        desiredCoursesListView.setAdapter(desiredCoursesArrayAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(SemesterInfo.getSEMESTER_INFO(), MODE_PRIVATE);

        // Load Semester Info from previous session
        String selectedSemesterString = preferences.getString("selectedSemester", null);
        if (selectedSemesterString != null) {
            try {
                selectedSemester = new SemesterInfo(new JSONObject(selectedSemesterString));
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error: Received Invalid Data", Toast.LENGTH_LONG).show();
                fetchSemesters();
                return;
            }
            updateDepartmentInfoAdapter(selectedSemester.getDepartmentArrayList());
        }
        else {
            fetchSemesters();
            return;
        }

        // Load list of desired courses from previous session
        String desiredCoursesString = preferences.getString("desiredCourses", null);
        if (desiredCoursesString != null){
            String[] desiredCoursesArray = desiredCoursesString.split(",");
            for (String string : desiredCoursesArray){
                String[] courseInfoStrings = string.split("-");
                SemesterInfo.DepartmentInfo.CourseInfo courseInfo = getCourseInfo(courseInfoStrings[0], courseInfoStrings[1]);
                desiredCoursesArrayList.add(courseInfo);
                Log.i("Desired Course", string);
                try {
                    Log.i("Desired Course", courseInfo.toJSON().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            desiredCoursesArrayAdapter.notifyDataSetChanged();
        }
        else
            Log.i("Desired Course", "No Desired Courses Found");


    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    protected void onPause() {
        super.onPause();
        String selectedSemesterString = null;
        SharedPreferences.Editor editor = getSharedPreferences(SemesterInfo.getSEMESTER_INFO(), MODE_PRIVATE).edit();
        if (selectedSemester != null){
            try {
                selectedSemesterString = selectedSemester.toJSON().toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            editor.putString("selectedSemester", selectedSemesterString);
            editor.apply();
            Log.i("Selected Semester Info", selectedSemesterString );
        }

        if (desiredCoursesArrayList != null)
            if (desiredCoursesArrayList.size() > 0){
                StringBuilder desiredCoursesString = new StringBuilder();
                for (SemesterInfo.DepartmentInfo.CourseInfo courseInfo : desiredCoursesArrayList){
                    desiredCoursesString.append(courseInfo.getDepartmentInfo().getDepartmentAcronym()).append("-").append(courseInfo.getCourseNumber()).append(",");
                }
                Log.i("Desired Courses Builder", desiredCoursesString.length() > 0 ? desiredCoursesString.substring( 0, desiredCoursesString.length() - 1 ): null);
                editor.putString("desiredCourses", desiredCoursesString.length() > 0 ? desiredCoursesString.substring( 0, desiredCoursesString.length() - 1 ): null);
                editor.apply();
            }
        if (desiredCoursesArrayList != null) {
            desiredCoursesArrayList.clear();
        }
    }


    public void addCourse(View view){

        if(selectedSemester == null){
            addCourse.setError("Please select a semester first");
            Toast.makeText(getApplicationContext(), "Please select a semester first", Toast.LENGTH_LONG).show();
            return;
        }

        addCourse.setError(null);

        String department = courseDepartment.getText().toString().toUpperCase();
        String number = courseNumber.getText().toString();

        if (department.equals("")){
            courseDepartment.setError("Department cannot be blank");
            courseDepartment.requestFocus();
            return;
        }
        if (number.equals("")){

            courseNumber.setError("Course number cannot be blank");
            courseNumber.requestFocus();
            return;
        }

        SemesterInfo.DepartmentInfo.CourseInfo selectedCourse = getCourseInfo(department, number);

        if (selectedCourse != null) {

            Log.d("Selected Course", selectedCourse.getDepartmentInfo().getDepartmentAcronym() + " - " + ((Integer) selectedCourse.getCourseNumber()).toString() + "\t" + selectedCourse.getCourseTitle());

            if(!desiredCoursesArrayList.contains(selectedCourse)) {
                desiredCoursesArrayList.add(selectedCourse);
                desiredCoursesArrayAdapter.notifyDataSetChanged();


                courseNumber.setError(null);

                courseDepartment.setText("");
                courseNumber.setText("");

                courseDepartment.requestFocus();
            }
            else
                //Toast.makeText(SelectCourses.this, "Class already selected", Toast.LENGTH_LONG).show();
                courseNumber.setError("Class already selected!");
                courseNumber.requestFocus();


        }
        else {
            //Toast.makeText(SelectCourses.this, "Class not found", Toast.LENGTH_LONG).show();

            courseNumber.setError("Class not found!");
            courseNumber.requestFocus();

            courseDepartment.requestFocus();
        }

    }

    public void getCourseSections(View view){

        if(!(desiredCoursesArrayList.size()>0)){
            courseDepartment.setError("You must have at least once course to build a schedule");
            courseDepartment.requestFocus();
            return;
        }

        StringBuilder semesterParam = new StringBuilder(URL_GET_COURSE_SECTIONS_PARAM_SEMESTER);
        StringBuilder departmentParam = new StringBuilder(URL_GET_COURSE_SECTIONS_PARAM_DEPARTMENT);
        StringBuilder courseNumberParam = new StringBuilder(URL_GET_COURSE_SECTIONS_PARAM_COURSENUMBER);
        //urlBuilder.append(((Integer) selectedSemester.getSemesterNumber()).toString() + "*");

        for (SemesterInfo.DepartmentInfo.CourseInfo courseInfo : desiredCoursesArrayList){
            semesterParam.append(selectedSemester.getSemesterNumber()).append(",");
            departmentParam.append(courseInfo.getDepartmentInfo().getDepartmentAcronym()).append(",");
            courseNumberParam.append(courseInfo.getCourseNumber()).append(",");
        }

        String semesterParamFinal = semesterParam.length() > 0 ? semesterParam.substring( 0, semesterParam.length() - 1 ): "";
        String departmentParamFinal = departmentParam.length() > 0 ? departmentParam.substring( 0, departmentParam.length() - 1 ): "";
        String courseNumberParamFinal = courseNumberParam.length() > 0 ? courseNumberParam.substring( 0, courseNumberParam.length() - 1 ): "";

        String urlFinal = URL_GET_COURSE_SECTIONS + semesterParamFinal + departmentParamFinal + courseNumberParamFinal;

        HTTPService.FetchURL(urlFinal, ACTION_GET_DESIRED_COURSE_SECTIONS, this);

        /* Deprecisted with use of HTTPService.FetchURL();
        Intent intent = new Intent(this, HTTPService.class);
        if(spoofServerSwitch) {
            intent.putExtra(HTTPService.REQUEST_URL, HTTPService.SPOOF_SERVER);
            intent.putExtra(HTTPService.SPOOFED_RESPONSE, SPOOF_DESIRED_COURSE_SECTIONS);
        }
        else
            intent.putExtra(HTTPService.REQUEST_URL, urlFinal);

        intent.putExtra(HTTPService.SOURCE_INTENT, ACTION_GET_DESIRED_COURSE_SECTIONS);
        startService(intent);
        */

        showProgressDialog("Getting All Selected Course Data");
    }

    public void getSemesterInfo(View view){

        Log.i("Get Semesters", "Getting Semesters");

        ArrayList <SemesterInfo> fileSemesters = loadSemesterInfoFromFile(SelectCourses.this);
        Log.i("Get Semesters", "Semesters found on file: " + fileSemesters.size());

        if (fileSemesters.size() == 0)
            fetchSemesters();
        else
            selectSemester(fileSemesters);

    }

    public SemesterInfo.DepartmentInfo.CourseInfo getCourseInfo(String department, String number){

        SemesterInfo.DepartmentInfo departmentInfo = getDepartmentInfo(department);
        for (SemesterInfo.DepartmentInfo.CourseInfo courseInfo : departmentInfo.getCourses()){
            if (((Integer) courseInfo.getCourseNumber()).toString().equals(number)){
                return courseInfo;
            }
        }
        return null;
    }

    public SemesterInfo.DepartmentInfo getDepartmentInfo(String department){

        if (selectedSemester != null)
        for(SemesterInfo.DepartmentInfo departmentInfo : selectedSemester.getDepartmentArrayList()){
            if(departmentInfo.getDepartmentAcronym().toUpperCase().equals(department.toUpperCase())){
                return departmentInfo;
            }
        }
        return null;
    }

    private void selectSemester(final ArrayList<SemesterInfo> semesterOptions){

        ArrayList<String> semesterTitles = new ArrayList<>(semesterOptions.size());

        for (SemesterInfo semesterInfo : semesterOptions){
            semesterTitles.add(semesterInfo.getSemesterNumber() + " - " + semesterInfo.getSemesterName());
        }

        ArrayAdapter<String> semesterTitlesAdapter = new ArrayAdapter<>(SelectCourses.this, android.R.layout.simple_selectable_list_item, semesterTitles);

        AlertDialog.Builder getDesiredSemester = new AlertDialog.Builder(SelectCourses.this);
        getDesiredSemester.setTitle("Please Select A Semester");
        getDesiredSemester.setAdapter(semesterTitlesAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setSelectedSemester(semesterOptions.get(which));
                dialog.dismiss();
            }
        });
        getDesiredSemester.setNeutralButton("UPDATE SEMESTERS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fetchSemesters();
                dialog.dismiss();
            }
        });
        getDesiredSemester.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        getDesiredSemester.create().show();
    }

    public void setSelectedSemester(SemesterInfo semesterInfo){
        this.selectedSemester = semesterInfo;
        updateDepartmentInfoAdapter(semesterInfo.getDepartmentArrayList());
    }

    private void fetchSemesters(){

        HTTPService.FetchURL(URL_GET_SEMESTER, ACTION_GET_SEMESTER, this);
        /* Deprecisted with use of HTTPService.FetchURL();
        Intent intent = new Intent(this, HTTPService.class);
        if(spoofServerSwitch) {
            intent.putExtra(HTTPService.REQUEST_URL, HTTPService.SPOOF_SERVER);
            intent.putExtra(HTTPService.SPOOFED_RESPONSE, SPOOF_SEMESTER);
        }
        else
            intent.putExtra(HTTPService.REQUEST_URL, URL_GET_SEMESTER);

        intent.putExtra(HTTPService.SOURCE_INTENT, ACTION_GET_SEMESTER);
        startService(intent);
        */
        showProgressDialog("Getting Semester Data");
    }

    public void selectBlockoutTimes(View view){
        Intent startSelectCoursesActivity = new Intent(SelectCourses.this, SelectBlockoutTimes.class);
        if (blockoutTimes != null) {
            Log.d("BLOCKOUT TIMES", blockoutTimes.toJSON().toString());
            startSelectCoursesActivity.putExtra("BLOCKOUT TIMES", blockoutTimes.toJSON().toString());
        }
        SelectCourses.this.startActivityForResult(startSelectCoursesActivity, 0);
    }

    private void updateDepartmentInfoAdapter(ArrayList<SemesterInfo.DepartmentInfo> departmentInfo){
        departmentInfoArrayAdapter = new DepartmentInfoArrayAdapter(this,R.layout.desired_courses_listview, departmentInfo);
        courseDepartment.setAdapter(departmentInfoArrayAdapter);
    }

    private void updateCourseInfoAdapter(ArrayList<SemesterInfo.DepartmentInfo.CourseInfo> courseInfo){
        courseInfoArrayAdapter = new CourseInfoArrayAdapter(this,R.layout.desired_courses_listview, courseInfo);
        courseNumber.setAdapter(courseInfoArrayAdapter);
    }

    private class DepartmentCoursesReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject response;
            boolean success;
            String message;

            if(progressDialog != null)
                progressDialog.dismiss();

            try {

                response = new JSONObject(intent.getStringExtra(HTTPService.SERVER_RESPONSE));
                success = response.getBoolean("Success");
                if(response.has("Message")) {
                    if(success)
                        message = response.getString("Message");
                    else message = "Error: " + response.getString("Message");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
                if(response.has("TimeTaken")){
                    float timeTaken = Float.parseFloat(response.getString("TimeTaken"));
                    Log.d("New Request Time Taken:", Float.toString(timeTaken));
                }
                if(success) {
                    ArrayList<SemesterInfo> fetchedSemesters = SemesterInfo.SemesterInfoFactory(response);
                    Log.i("Get Semesters", "Semesters found in fetch: " + fetchedSemesters.size());
                    SemesterInfo.saveSemestersToFile(fetchedSemesters, SelectCourses.this);

                    Toast.makeText(getBaseContext(), "Semester Data Updated", Toast.LENGTH_LONG).show();

                    selectSemester(fetchedSemesters);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class DesiredSectionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject response;
            boolean success;
            String message;

            if(progressDialog!= null)
                progressDialog.dismiss();

            final ArrayList<Course> fetchedCourses;
            ArrayList<Section> sectionArrayList = null;

            try {
                response = new JSONObject(intent.getStringExtra(HTTPService.SERVER_RESPONSE));
                success = response.getBoolean("Success");
                if(response.has("Message")) {
                    if(success)
                        message = response.getString("Message");
                    else message = "Error: " + response.getString("Message");
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
                if(response.has("TimeTaken")){
                    float timeTaken = Float.parseFloat(response.getString("TimeTaken"));
                    Log.d("New Request Time Taken:", Float.toString(timeTaken));
                }
                if(success) {
                    JSONArray jsonCourses = response.getJSONArray("Results");
                    float timeTaken = Float.parseFloat(response.getString("TimeTaken"));
                    Log.d("New Request Time Taken:", Float.toString(timeTaken));
                    fetchedCourses = Course.buildCourseList(jsonCourses);

                    try {
                        ArrayList<Section> blockoutSections;
                        if (blockoutTimes != null)
                            blockoutSections = blockoutTimes.getSectionList();
                        else
                            blockoutSections = new ArrayList<>();

                        if (fetchedCourses != null)
                            sectionArrayList = Schedule.scheduleGenerator(fetchedCourses, new ArrayList<Section>(), blockoutSections);
                        if (sectionArrayList != null) {
                            for (Section section : sectionArrayList){
                                Log.i("Built Schedule Sections",section.getSourceCourse().getCourseTitle() + " " + section.getSourceCourse().getCourseNumber() + "-" + section.getSectionNumber() + "\t" + section.toJSON().toString());
                            }
                        }

                        Schedule schedule = new Schedule("", selectedSemester.getSemesterNumber(), sectionArrayList);
                        DetailedSchedule.ShowSchedule(schedule, SelectCourses.this);
                        Log.i("Built Schedule", schedule.toJSON().toString());
                    } catch (NoSchedulesPossibleException e) {
                        e.printStackTrace();
                        AlertDialog.Builder noSchedulesPossible = new AlertDialog.Builder(SelectCourses.this);
                        noSchedulesPossible.setTitle("Schedule Could be generated. Issues:");
                        noSchedulesPossible.setMessage(e.printConflict());
                        noSchedulesPossible.setNeutralButton("CHANGE COURSES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        noSchedulesPossible.setPositiveButton("GENERATE ANYWAY", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Schedule schedule = new Schedule("", selectedSemester.getSemesterNumber(), Schedule.scheduleGenerator(fetchedCourses));
                                DetailedSchedule.ShowSchedule(schedule, SelectCourses.this);
                            }
                        });
                        noSchedulesPossible.create().show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String blockoutTimes;
        if (data == null)
            return;
        if (data.hasExtra("BLOCKOUT"))
            blockoutTimes = data.getStringExtra("BLOCKOUT");
        else
            return;
        try {
            JSONObject jsonBlockoutTimes = new JSONObject(blockoutTimes);
            this.blockoutTimes = new Course(jsonBlockoutTimes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (this.blockoutTimes != null)
            Log.d("test", this.blockoutTimes.toJSON().toString());
    }

    public static ArrayList<SemesterInfo> loadSemesterInfoFromFile(Context context){

        Log.i("Load Semester", "Preparing to load");

        SharedPreferences savedSemesters = context.getSharedPreferences(SemesterInfo.getSEMESTER_INFO(), MODE_PRIVATE);

        Map<String, ?> allEntries = savedSemesters.getAll();

        ArrayList<SemesterInfo> semesterInfoArrayList = new ArrayList<>(allEntries.size());

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {

            String semesterInfoString = entry.getValue().toString();
            if (entry.getKey().startsWith("SEMESTER_INFO_")) {
                try {
                    JSONObject semesterInfoJSON = new JSONObject(semesterInfoString);
                    SemesterInfo semesterInfo = new SemesterInfo(semesterInfoJSON);
                    semesterInfoArrayList.add(semesterInfo);
                    Log.i("Semester from File", entry.getKey() + " : " + semesterInfo.getSemesterNumber() + " : " + semesterInfoString);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("Load Semesters", "Failed to load Semester: " + entry.getKey());
                }
                Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
            }

        }

        return semesterInfoArrayList;

    }

    private void showProgressDialog(String title){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title);
        progressDialog.setMessage("Please wait while data is fetched...");
        progressDialog.show();
    }

}
