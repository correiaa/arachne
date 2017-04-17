/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.RoleRule;
import at.nieslony.openvpnadmin.RoleRuleIsMemberOfLdapGroup;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 *
 * @author claas
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class RoleRuleIsLdapUserFactory
        implements RoleRuleFactory, Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    private LdapSettings ldapSettings = null;

    @ManagedProperty(value = "#{roleRuleFactoryCollection}")
    private RoleRuleFactoryCollection roleRuleFactoryCollection;

    public void setRoleRuleFactoryCollection(RoleRuleFactoryCollection rrfc) {
        roleRuleFactoryCollection = rrfc;
    }

    private LdapSettings getLdapSettings() {
        if (ldapSettings == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            ldapSettings = (LdapSettings) context.getExternalContext().getApplicationMap().get("ldapSettings");
            if (ldapSettings == null)
                logger.severe("Cannot find attribute ldapSettings");
        }

        return ldapSettings;
    }
    /**
     * Creates a new instance of RoleRuleFactoryIsLdapUser
     */
    public RoleRuleIsLdapUserFactory() {
    }

    @PostConstruct
    public void init() {
        roleRuleFactoryCollection.addRoleRuleFactory(this);
    }

    @Override
    public RoleRule createRule(String groupname) {
        RoleRuleIsMemberOfLdapGroup rule = new RoleRuleIsMemberOfLdapGroup(this, groupname);

        return rule;
    }

    @Override
    public String getRoleRuleName() {
        return "isMemberOfLdapGroup";
    }

    @Override
    public String getDescriptionString() {
        return "Is member of LDAP group";
    }


    @Override
    public List<String> completeValue(String userPattern) {
        List<String> groups = new LinkedList<>();

        DirContext ctx;
        NamingEnumeration results;
        StringBuilder pattern = new StringBuilder();
        pattern.append("(&(");
            pattern.append("objectClass=").append(getLdapSettings().getObjectClassGroup());
        pattern.append(")(");
            pattern.append("|(");
            pattern.append(getLdapSettings().getAttrGroupName()).append("=").append(userPattern).append("*");
            pattern.append(")(");
            pattern.append(getLdapSettings().getAttrGroupDescription()).append("=*").append(userPattern).append("*");
            pattern.append("))");
        pattern.append(")");
        logger.info(String.format("Performing LDAP search %s", pattern.toString()));

        try {
            ctx = getLdapSettings().getLdapContext();
            SearchControls sc = new SearchControls();
            sc.setCountLimit(10);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(getLdapSettings().getOuUsers(), pattern.toString(), sc);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                if (attrs == null)
                    continue;
                Attribute attr = attrs.get(getLdapSettings().getAttrGroupName());
                if (attr == null)
                    continue;
                String gid = (String) attr.get();
                String description = (String) attr.get();
                StringBuilder group = new StringBuilder();
                group.append(gid);
                if (description != null && !description.isEmpty()) {
                    group.append(" (").append(description).append(")");
                }
                groups.add(group.toString());
            }
        }
        catch (NamingException ex) {
            logger.severe(String.format("An error occured during LDAP search: %s",
                    ex.getMessage()));
        }
        return groups;
    }
}