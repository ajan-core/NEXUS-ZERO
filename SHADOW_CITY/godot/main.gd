extends Node3D

# SHADOW CITY — playable 3D vertical-slice foundation.
# All prototype geometry is generated locally; no internet is needed at runtime.

var hero: CharacterBody3D
var camera: Camera3D
var yaw := 0.0
var pitch := -0.18
var health := 100
var xp := 0
var level := 1
var enemies: Array[Node3D] = []
var attack_cooldown := 0.0
var touch_move := Vector2.ZERO
var hud_label: Label
var hp_bar: ProgressBar
var touch_buttons: Dictionary = {}

const WALK_SPEED := 6.2
const JUMP_VELOCITY := 5.6
const MOUSE_SENS := 0.003

func _ready() -> void:
    _setup_input()
    _make_world()
    _make_hero()
    _make_camera()
    _make_hud()
    _spawn_enemies(5)

func _setup_input() -> void:
    # Keyboard actions are defined in project.godot; keep this safe for editor changes.
    for action in ["move_forward", "move_back", "move_left", "move_right", "jump", "attack"]:
        if not InputMap.has_action(action):
            InputMap.add_action(action)

func _material(color: Color, emission: Color = Color.BLACK) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    if emission != Color.BLACK:
        mat.emission_enabled = true
        mat.emission = emission
        mat.emission_energy_multiplier = 1.5
    return mat

func _box(parent: Node3D, name: String, size: Vector3, pos: Vector3, mat: Material, collision := false) -> StaticBody3D:
    var body := StaticBody3D.new() if collision else StaticBody3D.new()
    body.name = name
    body.position = pos
    parent.add_child(body)
    var mesh := MeshInstance3D.new()
    var shape := BoxMesh.new()
    shape.size = size
    mesh.mesh = shape
    mesh.material_override = mat
    body.add_child(mesh)
    if collision:
        var col := CollisionShape3D.new()
        var box_shape := BoxShape3D.new()
        box_shape.size = size
        col.shape = box_shape
        body.add_child(col)
    else:
        body.collision_layer = 0
        body.collision_mask = 0
    return body

func _make_world() -> void:
    var world_env := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.025, 0.018, 0.075)
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.48, 0.42, 0.78)
    env.ambient_light_energy = 0.8
    world_env.environment = env
    add_child(world_env)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-52, -28, 0)
    sun.light_color = Color(0.68, 0.62, 1.0)
    sun.light_energy = 1.25
    add_child(sun)

    var floor_mat := _material(Color(0.055, 0.045, 0.12))
    _box(self, "CityGround", Vector3(100, 1, 100), Vector3(0, -0.55, 0), floor_mat, true)
    var road_mat := _material(Color(0.035, 0.035, 0.075))
    _box(self, "NeonRoad", Vector3(12, 0.06, 90), Vector3(0, -0.015, 0), road_mat)
    var line_mat := _material(Color(0.24, 0.9, 1.0), Color(0.12, 0.8, 1.0))
    for z in range(-42, 45, 6):
        _box(self, "RoadDash", Vector3(0.12, 0.035, 2.5), Vector3(0, 0.03, z), line_mat)

    var building_colors := [
        Color(0.12, 0.075, 0.23), Color(0.075, 0.12, 0.25),
        Color(0.18, 0.065, 0.22), Color(0.07, 0.16, 0.2)
    ]
    var window_mat := _material(Color(0.35, 0.18, 0.72), Color(0.35, 0.08, 0.8))
    for side in [-1, 1]:
        for i in range(8):
            var z := -35.0 + float(i) * 10.0
            var x := float(side) * (12.0 + float(i % 3) * 2.5)
            var h := 7.0 + float((i * 7) % 13)
            var w := 5.0 + float(i % 2) * 2.0
            var building_mat := _material(building_colors[i % building_colors.size()])
            _box(self, "CityBlock_%s_%s" % [side, i], Vector3(w, h, 7), Vector3(x, h / 2.0, z), building_mat, true)
            for floor_i in range(max(1, int(h / 3.0))):
                var wy := 1.5 + float(floor_i) * 2.8
                if wy < h - 0.5:
                    _box(self, "NeonWindow", Vector3(0.12, 0.65, 1.4), Vector3(x - float(side) * (w/2.0 + 0.07), wy, z), window_mat)

    # glowing landmark arch at the far end of the starter district
    var cyan := _material(Color(0.1, 0.85, 1.0), Color(0.05, 0.65, 1.0))
    _box(self, "GateLeft", Vector3(0.35, 6, 0.35), Vector3(-4, 3, -39), cyan)
    _box(self, "GateRight", Vector3(0.35, 6, 0.35), Vector3(4, 3, -39), cyan)
    _box(self, "GateTop", Vector3(8.3, 0.35, 0.35), Vector3(0, 6, -39), cyan)

func _make_hero() -> void:
    hero = CharacterBody3D.new()
    hero.name = "AnimeHero"
    hero.position = Vector3(0, 1.2, 12)
    add_child(hero)
    var capsule := CapsuleMesh.new()
    capsule.radius = 0.42
    capsule.height = 1.8
    var body_mesh := MeshInstance3D.new()
    body_mesh.mesh = capsule
    body_mesh.material_override = _material(Color(0.13, 0.08, 0.27), Color(0.12, 0.02, 0.3))
    body_mesh.position.y = 0.9
    hero.add_child(body_mesh)
    var core := MeshInstance3D.new()
    var core_mesh := SphereMesh.new()
    core_mesh.radius = 0.22
    core_mesh.height = 0.44
    core.mesh = core_mesh
    core.material_override = _material(Color(0.1, 0.9, 1.0), Color(0.0, 0.7, 1.0))
    core.position = Vector3(0, 1.08, -0.38)
    hero.add_child(core)
    var col := CollisionShape3D.new()
    var shape := CapsuleShape3D.new()
    shape.radius = 0.42
    shape.height = 1.8
    col.shape = shape
    col.position.y = 0.9
    hero.add_child(col)

func _make_camera() -> void:
    var rig := Node3D.new()
    rig.name = "CameraRig"
    rig.position = Vector3(0, 1.5, 0)
    hero.add_child(rig)
    camera = Camera3D.new()
    camera.position = Vector3(0, 3.2, 7.8)
    camera.rotation_degrees.x = -15
    camera.current = true
    rig.add_child(camera)
    camera.fov = 72

func _make_hud() -> void:
    var ui := CanvasLayer.new()
    ui.name = "HUD"
    add_child(ui)
    var root := Control.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    ui.add_child(root)

    var title := Label.new()
    title.text = "SHADOW CITY  //  ANIME LEGENDS"
    title.position = Vector2(20, 18)
    title.add_theme_font_size_override("font_size", 18)
    title.add_theme_color_override("font_color", Color(0.75, 0.55, 1.0))
    root.add_child(title)

    hud_label = Label.new()
    hud_label.position = Vector2(20, 48)
    hud_label.add_theme_font_size_override("font_size", 15)
    hud_label.add_theme_color_override("font_color", Color(0.45, 0.95, 1.0))
    root.add_child(hud_label)

    hp_bar = ProgressBar.new()
    hp_bar.position = Vector2(20, 78)
    hp_bar.size = Vector2(180, 14)
    hp_bar.max_value = 100
    hp_bar.value = health
    hp_bar.show_percentage = false
    root.add_child(hp_bar)

    _add_touch_button(root, "▲", Vector2(28, -152), "move_forward")
    _add_touch_button(root, "◀", Vector2(8, -104), "move_left")
    _add_touch_button(root, "▼", Vector2(28, -56), "move_back")
    _add_touch_button(root, "▶", Vector2(76, -104), "move_right")
    _add_touch_button(root, "JUMP", Vector2(-138, -108), "jump")
    _add_touch_button(root, "BLAST", Vector2(-78, -58), "attack")
    _refresh_hud()

func _add_touch_button(root: Control, caption: String, offset: Vector2, action: String) -> void:
    var button := TouchScreenButton.new()
    button.name = "Touch_" + action
    var screen_size := get_viewport().get_visible_rect().size
    if offset.x >= 0:
        button.position = Vector2(24.0 + offset.x, screen_size.y - 190.0 + (offset.y + 152.0))
    else:
        button.position = Vector2(screen_size.x + offset.x, screen_size.y + offset.y)
    # TouchScreenButton uses an input action and is lightweight on Android.
    button.action = action
    var circle := CircleShape2D.new()
    circle.radius = 27
    button.shape = circle
    button.visibility_mode = TouchScreenButton.VISIBILITY_ALWAYS
    root.add_child(button)
    var label := Label.new()
    label.text = caption
    label.position = button.position - Vector2(18, 14)
    label.add_theme_font_size_override("font_size", 13)
    label.add_theme_color_override("font_color", Color(0.85, 0.8, 1.0))
    root.add_child(label)

func _refresh_hud() -> void:
    if is_instance_valid(hud_label):
        hud_label.text = "LEVEL %02d   •   XP %03d   •   SHARDS %03d" % [level, xp, xp / 5]
    if is_instance_valid(hp_bar):
        hp_bar.value = health

func _input(event: InputEvent) -> void:
    if event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_RIGHT):
        yaw -= event.relative.x * MOUSE_SENS
        hero.rotation.y = yaw
    if event is InputEventScreenDrag:
        # Drag on the right half to rotate the hero/camera direction.
        if event.position.x > get_viewport().get_visible_rect().size.x * 0.42:
            yaw -= event.relative.x * MOUSE_SENS * 1.8
            hero.rotation.y = yaw
    if event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
        get_tree().paused = not get_tree().paused

func _physics_process(delta: float) -> void:
    if not is_instance_valid(hero):
        return
    attack_cooldown = maxf(0.0, attack_cooldown - delta)
    var input_vec := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
    var basis := Basis(Vector3.UP, yaw)
    var direction := basis * Vector3(input_vec.x, 0, input_vec.y)
    if direction.length() > 0.01:
        hero.velocity.x = direction.x * WALK_SPEED
        hero.velocity.z = direction.z * WALK_SPEED
        hero.rotation.y = lerp_angle(hero.rotation.y, atan2(-direction.x, -direction.z), delta * 8.0)
    else:
        hero.velocity.x = move_toward(hero.velocity.x, 0, WALK_SPEED * 7 * delta)
        hero.velocity.z = move_toward(hero.velocity.z, 0, WALK_SPEED * 7 * delta)
    if not hero.is_on_floor():
        hero.velocity.y -= 18.0 * delta
    elif Input.is_action_just_pressed("jump"):
        hero.velocity.y = JUMP_VELOCITY
    if Input.is_action_just_pressed("attack") and attack_cooldown <= 0.0:
        _attack()
    hero.move_and_slide()
    _update_camera(delta)
    _update_enemies(delta)

func _update_camera(delta: float) -> void:
    var rig := hero.get_node_or_null("CameraRig") as Node3D
    if rig:
        rig.rotation.y = lerp_angle(rig.rotation.y, yaw, delta * 7.0)
        camera.position = camera.position.lerp(Vector3(0, 3.2, 7.8), delta * 3.0)
        camera.look_at(hero.global_position + Vector3(0, 1.0, 0), Vector3.UP)

func _spawn_enemies(count: int) -> void:
    for i in range(count):
        var enemy := Node3D.new()
        enemy.name = "ShadowDrone_%02d" % i
        enemy.position = Vector3(randf_range(-4.5, 4.5), 0.8, randf_range(-28, -5))
        add_child(enemy)
        var mesh := MeshInstance3D.new()
        var shape := SphereMesh.new()
        shape.radius = 0.55
        shape.height = 1.1
        mesh.mesh = shape
        mesh.material_override = _material(Color(1.0, 0.12, 0.55), Color(0.9, 0.02, 0.3))
        enemy.add_child(mesh)
        enemies.append(enemy)

func _update_enemies(delta: float) -> void:
    for enemy in enemies:
        if not is_instance_valid(enemy):
            continue
        var to_hero := hero.global_position - enemy.global_position
        to_hero.y = 0
        if to_hero.length() > 1.6:
            enemy.global_position += to_hero.normalized() * delta * 1.4

func _attack() -> void:
    attack_cooldown = 0.45
    var target: Node3D
    var best_distance := 4.0
    for enemy in enemies:
        if is_instance_valid(enemy):
            var d := hero.global_position.distance_to(enemy.global_position)
            if d < best_distance:
                best_distance = d
                target = enemy
    if is_instance_valid(target):
        enemies.erase(target)
        target.queue_free()
        xp += 15
        if xp >= level * 60:
            level += 1
            health = mini(100, health + 20)
        _refresh_hud()
        _spawn_enemies(1)
    else:
        # Keep an energy-pulse visual even when no enemy is close enough.
        var pulse := MeshInstance3D.new()
        var sphere := SphereMesh.new()
        sphere.radius = 1.1
        sphere.height = 2.2
        pulse.mesh = sphere
        pulse.material_override = _material(Color(0.15, 0.8, 1.0, 0.35), Color(0.05, 0.5, 1.0))
        pulse.position = hero.position + Vector3(0, 1, -1.2)
        add_child(pulse)
        get_tree().create_timer(0.16).timeout.connect(func(): if is_instance_valid(pulse): pulse.queue_free())
