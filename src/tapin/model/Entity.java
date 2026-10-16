package tapin.model;

/** Fixed metadata: SQL identifiers can never come from a text field. */
public enum Entity {
    STUDENTS("Students","students","student_id",new String[]{"student_no","rfid_uid","first_name","last_name","section_id","active"},new String[]{"Student number","RFID UID","First name","Last name","Section","Active"},new int[]{30,30,50,50,0,0}),
    SECTIONS("Sections","sections","section_id",new String[]{"section_name","course","year_level"},new String[]{"Section name","Course","Year level"},new int[]{30,50,10}),
    SUBJECTS("Subjects","subjects","subject_id",new String[]{"subject_code","subject_name"},new String[]{"Subject code","Subject name"},new int[]{20,100}),
    ACCOUNTS("Accounts","accounts","account_id",new String[]{"username","password","full_name","role"},new String[]{"Username","Password (blank keeps current)","Full name","Role"},new int[]{30,255,100,0}),
    SCHEDULES("Schedules","class_schedules","schedule_id",new String[]{"section_id","subject_id","teacher_id","day_of_week","start_time","end_time","grace_minutes"},new String[]{"Section","Subject","Teacher","Day","Start (HH:mm)","End (HH:mm)","Grace minutes"},new int[]{0,0,0,10,0,0,0});
    public final String title, table, id;
    public final String[] fields, labels;
    public final int[] lengths;
    Entity(String title,String table,String id,String[] fields,String[] labels,int[] lengths) {
        this.title=title;this.table=table;this.id=id;this.fields=fields;this.labels=labels;this.lengths=lengths;
    }
}
