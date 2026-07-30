package com.zillit.automation.core;

/**
 * Exact validation messages from the app's strings.xml — tests assert on these.
 * Substrings are used (textContains) so trailing punctuation changes don't break tests.
 */
public final class Messages {

    public static final String ALL_REQUIRED = "All Fields are required";
    public static final String FIRST_NAME_EMPTY = "First Name cannot be empty";
    public static final String FIRST_NAME_LENGTH = "First Name should be of at least 3 characters";
    public static final String LAST_NAME_EMPTY = "Last Name cannot be empty";
    public static final String LAST_NAME_LENGTH = "Last Name should be of at least 3 characters";
    public static final String SPECIAL_CHARS = "Special characters are not allowed";
    public static final String SELECT_PROJECT_TYPE = "Please Select Project Type";
    public static final String SELECT_PROJECT_SUBTYPE = "Please Select Project Subtype";
    public static final String SUBTYPE_LENGTH = "Project Subtype should be of at least 3 characters";
    public static final String SELECT_LANGUAGE = "Please select project language";
    public static final String PROJECT_NAME_EMPTY = "Project name cannot be empty";
    public static final String PROJECT_NAME_LENGTH = "Project name should be of at least 3 characters";
    public static final String EMAIL_EMPTY = "Email cannot be Empty";
    public static final String EMAIL_INVALID = "Please input valid email address";
    public static final String EMAIL_NOT_VERIFIED = "Please click Verify button to authenticate email";
    public static final String COUNTRY_CODE_REQUIRED = "Country code is required";
    public static final String PHONE_REQUIRED = "Phone Number is required";
    public static final String ACCEPT_TERMS = "Please Accept Terms and Condition";
    public static final String PROJECT_CODE_EMPTY = "Project Code Cannot be Empty";
    public static final String SELECT_DEPARTMENT = "Please select your department";
    public static final String SELECT_DEPARTMENT_FIRST = "Please select department first";
    public static final String SELECT_DESIGNATION = "Please select designation";
    public static final String SELECT_UNIT = "Please select unit";
    public static final String TAKE_SELFIE = "Please take a selfie";

    // Home chat
    public static final String TYPE_MESSAGE_TO_SEND = "Please Type Message To Send";
    public static final String WORD_LIMIT = "Characters limit allowed to send"; // "…only 2000 Characters…"
    public static final String NO_POSTING_RIGHTS = "Posting Rights";
    public static final String CHOOSE_BELOW = "Choose below";
    public static final int TEXT_LIMIT = 2000;
    // Call-sheet document-upload popups
    public static final String CALL_SHEET_FIRST = "continuation of the existing Call Sheet";
    public static final String CALL_SHEET_REPLACE = "replaced with the new upload";
    public static final String VERIFIED = "Verified";
    public static final String VERIFY = "Verify";

    private Messages() {
    }
}
