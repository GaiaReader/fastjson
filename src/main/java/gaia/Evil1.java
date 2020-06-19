package gaia;

public class Evil1 {
    String cmd;
    public Evil1(){}

    public String getCmd() {
        try{
            Runtime.getRuntime().exec(cmd);
        }catch (Exception e){
            e.printStackTrace();
        }
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;

    }
}
