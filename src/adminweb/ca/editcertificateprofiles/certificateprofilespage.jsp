<%
  TreeMap certificateprofiles     = cabean.getEditCertificateProfileNames(); 

  
%>


<div align="center">
  <p><H1><%= ejbcawebbean.getText("EDITCERTIFICATEPROFILES") %></H1></p>
 <!-- <div align="right"><A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ca_help.html") + "#certificateprofiles"%>")'>
    <u><%= ejbcawebbean.getText("HELP") %></u> </A> -->
  </div>
  <form name="editcertificateprofiles" method="post"  action="<%= THIS_FILENAME%>">
    <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_EDIT_CERTIFICATEPROFILES %>'>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
    <% if(triedtoeditfixedcertificateprofile){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("YOUCANTEDITFIXEDCERTPROFS") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(triedtodeletefixedcertificateprofile){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("YOUCANTDELETEFIXEDCERT") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(triedtoaddfixedcertificateprofile){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("YOUCANTADDFIXEDCERT") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(certificateprofileexists){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("CERTIFICATEPROFILEALREADY") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(certificateprofiledeletefailed){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("COULDNTDELETECERTPROF") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H3><%= ejbcawebbean.getText("CURRENTCERTIFICATEPROFILES") %></H3></td>
        <td width="35%"></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%">
          <select name="<%=SELECT_CERTIFICATEPROFILES%>" size="15"  >
            <% Iterator iter = certificateprofiles.keySet().iterator();
               while(iter.hasNext()){
                 String profilename = (String) iter.next(); %>
                 
              <option value="<%=profilename%>"> 
                  <%= profilename %>
               </option>
            <%}%>
              <option value="">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
          </select>
          </td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%"> 
          <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
              <td>
                <input type="submit" name="<%= BUTTON_EDIT_CERTIFICATEPROFILES %>" value="<%= ejbcawebbean.getText("EDITCERTPROF") %>">
              </td>
              <td>
             &nbsp; 
              </td>
              <td>
                <input class=buttonstyle type="submit" onClick="return confirm('<%= ejbcawebbean.getText("AREYOUSURE") %>');" name="<%= BUTTON_DELETE_CERTIFICATEPROFILES %>" value="<%= ejbcawebbean.getText("DELETECERTPROF") %>">
              </td>
            </tr>
          </table> 
        </td>
        <td width="35%"> </td>
      </tr>
    </table>
   
  <p align="left"> </p>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr> 
        <td width="5%"></td>
        <td width="95%"><H3><%= ejbcawebbean.getText("ADD") %></H3></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="95%"> 
          <input type="text" name="<%=TEXTFIELD_CERTIFICATEPROFILESNAME%>" size="40" maxlength="255">   
          <input type="submit" name="<%= BUTTON_ADD_CERTIFICATEPROFILES%>" onClick='return checkfieldforlegalchars("document.editcertificateprofiles.<%=TEXTFIELD_CERTIFICATEPROFILESNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("ADD") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= BUTTON_RENAME_CERTIFICATEPROFILES%>" onClick='return checkfieldforlegalchars("document.editcertificateprofiles.<%=TEXTFIELD_CERTIFICATEPROFILESNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("RENAMESELECTED") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= BUTTON_CLONE_CERTIFICATEPROFILES%>" onClick='return checkfieldforlegalchars("document.editcertificateprofiles.<%=TEXTFIELD_CERTIFICATEPROFILESNAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("USESELECTEDASTEMPLATE") %>">
        </td>
      </tr>
      <tr> 
        <td width="5%">&nbsp; </td>
        <td width="95%">&nbsp;</td>
      </tr>
    </table>
  </form>
  <p align="center">&nbsp;</p>
  <p>&nbsp;</p>
</div>

