package herrcult69.compnet;

public class ResponseData {
	private String statusCode; 
	private String message; // the Final/ Ending from response
	private String data; // the data from dataSocket if have, null if control socket only

	public ResponseData() {
	}

	public ResponseData(String statusCode, String message, String data) {
		this.statusCode = statusCode;
		this.message = message;
		this.data = data;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean isSuccess() {
		return statusCode != null && (statusCode.startsWith("1") || statusCode.startsWith("2") || statusCode.startsWith("3"));
	}

	public static String extractStatusCode(String response) {
		if (response == null || response.isEmpty()) {
			return null;
		}

		String trimmed = response.trim();
		StringBuilder code = new StringBuilder();
		for (int i = 0; i < trimmed.length(); i++) {
			char c = trimmed.charAt(i); // loop thru, from begining if it is a digit then add it to the code variable via StringBuilder, then turn it to String if its not 0 length
			if (Character.isDigit(c)) {
				code.append(c);
			} else {
				break;
			}
		}

		return code.length() == 0 ? null : code.toString();
	}
}
