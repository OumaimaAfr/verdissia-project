package com.verdissia.dto.response;

public class ResponseDTO<T> implements IResponseDTO<T> {

    private String status;

    private T data;

    private CodeMessage error;

    private CodeMessage info;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public CodeMessage getError() {
        return error;
    }

    public void setError(CodeMessage error) {
        this.error = error;
    }

    public CodeMessage getInfo() {
        return info;
    }

    public void setInfo(CodeMessage info) {
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResponseDTO<?> that = (ResponseDTO<?>) o;
        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }
        if (data != null ? !data.equals(that.data) : that.data != null) {
            return false;
        }
        if (error != null ? !error.equals(that.error) : that.error != null) {
            return false;
        }
        return info != null ? info.equals(that.info) : that.info == null;
    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (info != null ? info.hashCode() : 0);
        return result;
    }

    public static class CodeMessage {
        private String code;
        private String message;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CodeMessage that = (CodeMessage) o;
            if (code != null ? !code.equals(that.code) : that.code != null) {
                return false;
            }
            return message != null ? message.equals(that.message) : that.message == null;
        }

        @Override
        public int hashCode() {
            int result = code != null ? code.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }
    }
}