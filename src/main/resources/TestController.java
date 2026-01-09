@RestController
@RequestMapping("/api/user")
public class UserController {

    private String abc;

    @Resource
    private UserService userService;

    public void bcas(String abc) {
        this.abc = abc;
    }

    public String wbj() {
        return abc;
    }

    @GetMapping("/list")
    public List<User> list() {}

    @PostMapping(value = "/add")
    public void add() {}

    @PutMapping(value = "/edit")
    public void edit() {}

    @DeleteMapping(value = "/delete")
    public void delete() {}

    @RequestMapping(
            value = {"/detail", "/info"},
            method = RequestMethod.GET
    )
    public User detail() {}


    @DeleteMapping(
            value = {"/delete1", "/delete2"}
    )
    public void deleteAll() {}
}