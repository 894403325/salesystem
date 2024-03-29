package com.business.system.controller;

import com.business.system.model.UserCreateForm;
import com.business.system.model.UserCreateFormValidator;
import com.business.system.service.UserService;
import com.common.constant.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @program: saleSystem
 * @description: 用户控制层，接收对当前用户操作的请求
 * @author: chengy
 * @create: 2018-11-14 09:18
 **/
@Controller
public class CurrentUserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserController.class);
    private final UserService userService;
    private final UserCreateFormValidator userCreateFormValidator;

    @Autowired
    public CurrentUserController(UserService userService, UserCreateFormValidator userCreateFormValidator) {
        this.userService = userService;
        this.userCreateFormValidator = userCreateFormValidator;
    }

    /**
     * 修改信息页面
     * @return system/currentUserDetail
     */
    @RequestMapping("/currentUser/getCurrentUserDetailPage")
    public String getCurrentUserDetailPage() {
        return "system/currentUserDetail";
    }

    @RequestMapping("/currentUser/saveCurrentUserInfo")
    @ResponseBody
    public JsonResult saveCurrentUserInfo(@RequestParam Map<String, String> parMap) {
        return new JsonResult(JsonResult.ERROR, "修改成功！");
    }


    @InitBinder("form")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(userCreateFormValidator);
    }

    @PreAuthorize("@currentUserServiceImpl.canAccessUser(principal, #id)")
    @RequestMapping("/user/{id}")
    public ModelAndView getUserPage(@PathVariable String id) {
        LOGGER.info("Getting user page for user={}", id);
        return new ModelAndView("user", "user", userService.getUserById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format("User=%s not found", id))));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "/user/create", method = RequestMethod.GET)
    public ModelAndView getUserCreatePage() {
        LOGGER.info("Getting user create form");
        return new ModelAndView("user_create", "form", new UserCreateForm());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "/user/create", method = RequestMethod.POST)
    public String handleUserCreateForm(@Valid @ModelAttribute("form") UserCreateForm form, BindingResult bindingResult) {
        LOGGER.info("Processing user create form={}, bindingResult={}", form, bindingResult);
        if (bindingResult.hasErrors()) {
            // failed validation
            return "user_create";
        }
        try {
            userService.create(form);
        } catch (DataIntegrityViolationException e) {
            // probably email already exists - very rare case when multiple admins are adding same user
            // at the same time and form validation has passed for more than one of them.
            LOGGER.info("Exception occurred when trying to save the user, assuming duplicate email", e);
            bindingResult.reject("email.exists", "Email already exists");
            return "user_create";
        }
        // ok, redirect
        return "redirect:/users";
    }

}
