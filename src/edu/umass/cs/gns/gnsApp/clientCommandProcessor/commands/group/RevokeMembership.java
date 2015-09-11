/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group;

import edu.umass.cs.gns.gnsApp.clientCommandProcessor.commandSupport.CommandResponse;
import static edu.umass.cs.gns.gnsApp.clientCommandProcessor.commandSupport.GnsProtocolDefs.*;
import edu.umass.cs.gns.gnsApp.clientCommandProcessor.commandSupport.GroupAccess;
import edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.CommandModule;
import edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.GnsCommand;
import edu.umass.cs.gns.gnsApp.clientCommandProcessor.demultSupport.ClientRequestHandlerInterface;
import edu.umass.cs.gns.utils.ResultValue;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author westy
 */
@Deprecated
public class RevokeMembership extends GnsCommand {

  /**
   *
   * @param module
   */
  public RevokeMembership(CommandModule module) {
    super(module);
  }

  @Override
  public String[] getCommandParameters() {
    return new String[]{GUID, MEMBER, SIGNATURE, SIGNATUREFULLMESSAGE};
  }

  @Override
  public String getCommandName() {
    return REVOKEMEMBERSHIP;
  }

  @Override
  public CommandResponse<String> execute(JSONObject json, ClientRequestHandlerInterface handler) throws InvalidKeyException, InvalidKeySpecException,
          JSONException, NoSuchAlgorithmException, SignatureException {
    String guid = json.getString(GUID);
    String member = json.getString(MEMBER);
    // signature and message can be empty for unsigned cases
    String signature = json.optString(SIGNATURE, null);
    String message = json.optString(SIGNATUREFULLMESSAGE, null);
    if (GroupAccess.revokeMembership(guid, new ResultValue(new ArrayList<Object>(Arrays.asList(member))), guid, signature, 
            message, handler)) {
      return new CommandResponse<String>(OKRESPONSE);
    } else {
      return new CommandResponse<String>(BADRESPONSE + " " + GENERICERROR);
    }
  }

  @Override
  public String getCommandDescription() {
    return "Revokes membership of member in the group. Guid needs to sign the command.";
  }
}
