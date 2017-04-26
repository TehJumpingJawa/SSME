package org.tjj.starsector.ssme.installer;

import java.lang.management.ManagementFactory;

public class ProcessID {
   public static void main(String[] args) {
      getProcessId();
   }

   public static String getProcessId() {
      String pname = ManagementFactory.getRuntimeMXBean().getName();
      System.out.println("process name = " + pname);
      String pid = pname;
      int i = pname.indexOf("@");
      if (i!=-1) pid = pname.substring(0,i);
      System.out.println("process id = " + pid);
      return pid;
   }

}