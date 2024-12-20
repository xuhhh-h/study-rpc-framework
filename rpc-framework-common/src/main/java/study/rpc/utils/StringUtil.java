package study.rpc.utils;

public class StringUtil {
    // 判断字符串是否为空或仅包含空白字符
    public static boolean isBlank(String s){
        if(s==null || s.length()==0){
            return true;
        }
        for(int i=0; i<s.length(); ++i){
            if(!Character.isWhitespace(s.charAt(i))){
                return false;
            }
        }
        return true;
    }
}
