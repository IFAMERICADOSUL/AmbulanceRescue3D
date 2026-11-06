extends Node3D

const SAVE_PATH := "user://frontiera_save.json"
const SAVE_INTERVAL := 120.0
const WORLD_SIZE := 90.0

var player: CharacterBody3D
var camera: Camera3D
var camera_pivot: Node3D
var camera_yaw := 0.0
var camera_pitch := -0.82
var camera_distance := 18.0
var look_touch_id := -1
var last_look_pos := Vector2.ZERO
var mouse_looking := false
var hud: CanvasLayer
var stats := {"vita": 100.0, "acqua": 85.0, "cibo": 85.0, "energia": 100.0, "bisogno": 0.0}
var inventory := {"Legno": 0, "Pietra": 0, "Fibra": 0, "Bacche": 0, "Acqua": 0, "Rottami": 0, "Carne": 0, "Pelle": 0, "Erbe": 0, "Munizioni": 0}
var structures: Array[Dictionary] = []
var selected_character := "Esploratore"
var build_choice := "Muro"
var game_started := false
var elapsed := 0.0
var save_elapsed := 0.0
var save_dot: ColorRect
var status_label: Label
var inventory_label: Label
var stats_label: Label
var action_label: Label
var build_menu: OptionButton
var nearby: Node3D
var enemies: Array[CharacterBody3D] = []
var touch_vector := Vector2.ZERO
var joystick_origin := Vector2.ZERO
var joystick_active := false
var menu_root: Control
var game_ui: Control
var rng := RandomNumberGenerator.new()

func _ready() -> void:
	rng.randomize()
	_build_menu()

func _process(delta: float) -> void:
	if not game_started:
		return
	elapsed += delta
	save_elapsed += delta
	_update_needs(delta)
	_update_camera()
	_update_enemies(delta)
	_update_hud()
	if save_elapsed >= SAVE_INTERVAL:
		save_game(true)
		save_elapsed = 0.0

func _physics_process(delta: float) -> void:
	if not game_started or player == null:
		return
	var direction := Vector2(
		Input.get_action_strength("move_right") - Input.get_action_strength("move_left"),
		Input.get_action_strength("move_down") - Input.get_action_strength("move_up")
	)
	if touch_vector.length() > 0.1:
		direction = touch_vector
	direction = direction.normalized()
	var move := Vector3(direction.x, 0, direction.y)
	if camera_pivot:
		var camera_right := camera_pivot.global_basis.x
		var camera_forward := -camera_pivot.global_basis.z
		camera_right.y = 0
		camera_forward.y = 0
		move = (camera_right.normalized() * direction.x + camera_forward.normalized() * direction.y).normalized()
	player.velocity.x = move.x * 5.0
	player.velocity.z = move.z * 5.0
	if move.length() > 0.1:
		player.look_at(player.global_position + move, Vector3.UP)
	player.move_and_slide()
	if Input.is_action_just_pressed("interact"):
		_interact()

func _build_menu() -> void:
	menu_root = Control.new()
	menu_root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(menu_root)
	var bg := ColorRect.new()
	bg.color = Color(0.055, 0.075, 0.065, 1)
	bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	menu_root.add_child(bg)
	var panel := VBoxContainer.new()
	panel.set_anchors_preset(Control.PRESET_CENTER)
	panel.position = Vector2(-220, -210)
	panel.size = Vector2(440, 420)
	panel.add_theme_constant_override("separation", 18)
	menu_root.add_child(panel)
	var title := Label.new()
	title.text = "FRONTIERA SELVAGGIA"
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	title.add_theme_font_size_override("font_size", 30)
	panel.add_child(title)
	var subtitle := Label.new()
	subtitle.text = "Sopravvivi. Raccogli. Costruisci. Resisti."
	subtitle.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	panel.add_child(subtitle)
	var character_title := Label.new()
	character_title.text = "Scegli il personaggio"
	panel.add_child(character_title)
	var chars := OptionButton.new()
	for c in ["Esploratore", "Meccanica", "Medico", "Cacciatrice"]:
		chars.add_item(c)
	panel.add_child(chars)
	chars.item_selected.connect(func(i): selected_character = chars.get_item_text(i))
	var start := Button.new()
	start.text = "NUOVA PARTITA"
	start.custom_minimum_size.y = 54
	panel.add_child(start)
	start.pressed.connect(_start_new_game)
	var load_btn := Button.new()
	load_btn.text = "CARICA PARTITA"
	panel.add_child(load_btn)
	load_btn.pressed.connect(load_game)
	var hint := Label.new()
	hint.text = "WASD / joystick • E o pulsante AZIONE per interagire"
	hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	panel.add_child(hint)

func _start_new_game() -> void:
	stats = {"vita": 100.0, "acqua": 85.0, "cibo": 85.0, "energia": 100.0, "bisogno": 0.0}
	inventory = {"Legno": 0, "Pietra": 0, "Fibra": 0, "Bacche": 0, "Acqua": 0, "Rottami": 0, "Carne": 0, "Pelle": 0, "Erbe": 0, "Munizioni": 0}
	structures.clear()
	elapsed = 0.0
	_create_world()
	menu_root.queue_free()
	_create_hud()
	game_started = true

func _create_world() -> void:
	var ground := MeshInstance3D.new()
	var plane := PlaneMesh.new()
	plane.size = Vector2(WORLD_SIZE, WORLD_SIZE)
	ground.mesh = plane
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.19, 0.28, 0.18)
	mat.roughness = 1.0
	ground.material_override = mat
	add_child(ground)
	var env := WorldEnvironment.new()
	var environment := Environment.new()
	environment.background_mode = Environment.BG_SKY
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color(0.65, 0.72, 0.62)
	environment.ambient_light_energy = 0.8
	env.environment = environment
	add_child(env)
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-55, -30, 0)
	sun.light_energy = 1.25
	add_child(sun)
	player = CharacterBody3D.new()
	player.name = "Giocatore"
	var body := MeshInstance3D.new()
	var capsule := CapsuleMesh.new()
	capsule.radius = 0.42
	capsule.height = 1.8
	body.mesh = capsule
	var skin := StandardMaterial3D.new()
	skin.albedo_color = Color(0.72, 0.48, 0.31) if selected_character != "Medico" else Color(0.82, 0.72, 0.56)
	body.material_override = skin
	body.position.y = 0.95
	player.add_child(body)
	var collision := CollisionShape3D.new()
	var shape := CapsuleShape3D.new()
	shape.radius = 0.42
	shape.height = 1.8
	collision.shape = shape
	collision.position.y = 0.9
	player.add_child(collision)
	player.position = Vector3(0, 0.05, 0)
	add_child(player)
	camera_pivot = Node3D.new()
	camera_pivot.name = "RotazioneTelecamera"
	camera_pivot.position = Vector3(0, 0.8, 0)
	player.add_child(camera_pivot)
	camera = Camera3D.new()
	camera.name = "TelecameraOrbitale"
	camera.position = Vector3(0, 0, camera_distance)
	camera.current = true
	camera.fov = 55.0
	camera_pivot.add_child(camera)
	_update_camera_transform()
	for i in range(115):
		var p := Vector3(rng.randf_range(-WORLD_SIZE/2, WORLD_SIZE/2), 0, rng.randf_range(-WORLD_SIZE/2, WORLD_SIZE/2))
		if p.length() < 8:
			continue
		_spawn_resource(p, ["Albero", "Roccia", "Cespuglio", "Rottami"][rng.randi_range(0, 3)])
	for i in range(4):
		_spawn_enemy(Vector3(rng.randf_range(-30,30), 0, rng.randf_range(-30,30)))
	# Punti di interesse: una piccola radura iniziale e risorse distribuite in modo casuale.
	_spawn_resource(Vector3(7, 0, -5), "Cespuglio")
	_spawn_resource(Vector3(-8, 0, 6), "Rottami")

func _spawn_resource(pos: Vector3, kind: String) -> void:
	var root := Area3D.new()
	root.position = pos
	root.set_meta("kind", kind)
	var mesh_instance := MeshInstance3D.new()
	var mesh: Mesh
	var color := Color.WHITE
	match kind:
		"Albero":
			mesh = CylinderMesh.new()
			(mesh as CylinderMesh).top_radius = 0.15
			(mesh as CylinderMesh).bottom_radius = 0.35
			(mesh as CylinderMesh).height = 2.8
			color = Color(0.20, 0.34, 0.18)
		"Roccia":
			mesh = SphereMesh.new()
			(mesh as SphereMesh).radius = 0.7
			(mesh as SphereMesh).height = 1.0
			color = Color(0.42, 0.43, 0.40)
		"Cespuglio":
			mesh = SphereMesh.new()
			(mesh as SphereMesh).radius = 0.65
			(mesh as SphereMesh).height = 0.8
			color = Color(0.28, 0.42, 0.18)
		_:
			mesh = BoxMesh.new()
			(mesh as BoxMesh).size = Vector3(0.9, 0.6, 0.7)
			color = Color(0.35, 0.38, 0.40)
	mesh_instance.mesh = mesh
	mesh_instance.position.y = 1.0 if kind == "Albero" else 0.45
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.roughness = 0.92
	mesh_instance.material_override = material
	root.add_child(mesh_instance)
	var collision := CollisionShape3D.new()
	var sphere := SphereShape3D.new()
	sphere.radius = 1.25
	collision.shape = sphere
	root.add_child(collision)
	add_child(root)

func _spawn_enemy(pos: Vector3) -> void:
	var enemy := CharacterBody3D.new()
	enemy.position = pos
	var mesh := MeshInstance3D.new()
	var shape_mesh := CapsuleMesh.new()
	shape_mesh.radius = 0.48
	shape_mesh.height = 1.7
	mesh.mesh = shape_mesh
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.34, 0.16, 0.13)
	mesh.material_override = mat
	mesh.position.y = 0.85
	enemy.add_child(mesh)
	var col := CollisionShape3D.new()
	var cap := CapsuleShape3D.new()
	cap.radius = 0.48
	cap.height = 1.7
	col.shape = cap
	col.position.y = 0.85
	enemy.add_child(col)
	add_child(enemy)
	enemies.append(enemy)

func _create_hud() -> void:
	hud = CanvasLayer.new()
	add_child(hud)
	game_ui = Control.new()
	game_ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	hud.add_child(game_ui)
	var top := PanelContainer.new()
	top.position = Vector2(14, 12)
	top.size = Vector2(370, 112)
	game_ui.add_child(top)
	var box := VBoxContainer.new()
	top.add_child(box)
	stats_label = Label.new()
	stats_label.add_theme_font_size_override("font_size", 15)
	box.add_child(stats_label)
	inventory_label = Label.new()
	inventory_label.add_theme_font_size_override("font_size", 13)
	box.add_child(inventory_label)
	status_label = Label.new()
	status_label.text = "Esplora e raccogli risorse"
	box.add_child(status_label)
	var right := VBoxContainer.new()
	right.anchor_left = 1
	right.anchor_right = 1
	right.offset_left = -190
	right.offset_right = -12
	right.offset_top = 12
	right.add_theme_constant_override("separation", 8)
	game_ui.add_child(right)
	var build_title := Label.new()
	build_title.text = "COSTRUZIONE"
	right.add_child(build_title)
	build_menu = OptionButton.new()
	for item in ["Pavimento", "Muro", "Porta", "Finestra", "Tetto", "Fuoco", "Armadio", "Banco da lavoro", "Letto", "Fornace", "Rastrelliera"]:
		build_menu.add_item(item)
	right.add_child(build_menu)
	build_menu.item_selected.connect(func(i): build_choice = build_menu.get_item_text(i))
	_add_button(right, "COSTRUISCI", _build_selected)
	_add_button(right, "CREA ASCE / ARMI", _craft_axe)
	_add_button(right, "AZIONE / RACCOGLI", _interact)
	_add_button(right, "MANGIA / BEVI", _consume)
	_add_button(right, "RIPOSA", _rest)
	_add_button(right, "INVENTARIO", _show_inventory)
	_add_button(right, "SALVA ORA", func(): save_game(false))
	var camera_hint := Label.new()
	camera_hint.text = "Trascina a destra per ruotare • pizzica per zoom"
	camera_hint.anchor_left = 0.5
	camera_hint.anchor_right = 0.5
	camera_hint.anchor_top = 1
	camera_hint.anchor_bottom = 1
	camera_hint.offset_left = -180
	camera_hint.offset_right = 180
	camera_hint.offset_top = -34
	camera_hint.offset_bottom = -12
	camera_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	game_ui.add_child(camera_hint)
	var left_bottom := VBoxContainer.new()
	left_bottom.anchor_top = 1
	left_bottom.anchor_bottom = 1
	left_bottom.offset_left = 14
	left_bottom.offset_top = -150
	left_bottom.offset_bottom = -14
	game_ui.add_child(left_bottom)
	_add_button(left_bottom, "▲", func(): touch_vector = Vector2(0,-1))
	_add_button(left_bottom, "◀   ●   ▶", func(): touch_vector = Vector2.ZERO)
	_add_button(left_bottom, "▼", func(): touch_vector = Vector2(0,1))
	var dot_button := Button.new()
	dot_button.text = "●"
	dot_button.tooltip_text = "Salvataggio automatico ogni 2 minuti"
	dot_button.anchor_left = 1
	dot_button.anchor_right = 1
	dot_button.anchor_top = 1
	dot_button.anchor_bottom = 1
	dot_button.offset_left = -58
	dot_button.offset_right = -12
	dot_button.offset_top = -60
	dot_button.offset_bottom = -14
	dot_button.add_theme_font_size_override("font_size", 25)
	game_ui.add_child(dot_button)
	dot_button.pressed.connect(func(): save_game(false))
	save_dot = ColorRect.new()
	save_dot.color = Color(0.2,0.8,0.35,1)
	save_dot.size = Vector2(10,10)
	save_dot.position = Vector2(-28,-30)
	dot_button.add_child(save_dot)

func _add_button(parent: Control, label: String, callback: Callable) -> void:
	var b := Button.new()
	b.text = label
	b.custom_minimum_size = Vector2(150, 42)
	parent.add_child(b)
	b.pressed.connect(callback)

func _update_needs(delta: float) -> void:
	stats["cibo"] = maxf(0, stats["cibo"] - delta * 0.035)
	stats["acqua"] = maxf(0, stats["acqua"] - delta * 0.055)
	stats["energia"] = maxf(0, stats["energia"] - delta * (0.018 if player.velocity.length() > 1 else 0.008))
	stats["bisogno"] = minf(100, stats["bisogno"] + delta * 0.018)
	if stats["acqua"] <= 0 or stats["cibo"] <= 0:
		stats["vita"] = maxf(0, stats["vita"] - delta * 0.12)
	if stats["energia"] <= 0:
		stats["vita"] = maxf(0, stats["vita"] - delta * 0.04)
	if stats["vita"] <= 0:
		status_label.text = "Sei svenuto. Ricarica un salvataggio o ricomincia."

func _update_camera() -> void:
	if camera_pivot and camera:
		_update_camera_transform()

func _update_camera_transform() -> void:
	if camera_pivot == null or camera == null:
		return
	camera_pivot.rotation = Vector3(camera_pitch, camera_yaw, 0)
	camera.position = Vector3(0, 0, camera_distance)
	camera.look_at(camera_pivot.global_position, Vector3.UP)

func _update_enemies(delta: float) -> void:
	for enemy in enemies:
		if not is_instance_valid(enemy):
			continue
		var to_player := player.global_position - enemy.global_position
		if to_player.length() < 17:
			var dir := to_player.normalized()
			enemy.velocity.x = dir.x * 2.2
			enemy.velocity.z = dir.z * 2.2
			enemy.move_and_slide()
			if to_player.length() < 1.6:
				stats["vita"] = maxf(0, stats["vita"] - delta * 5.0)

func _update_hud() -> void:
	stats_label.text = "❤ Vita %d   💧 Acqua %d   🍖 Cibo %d\n⚡ Energia %d   Bisogno %d" % [int(stats.vita), int(stats.acqua), int(stats.cibo), int(stats.energia), int(stats.bisogno)]
	inventory_label.text = "Zaino: " + "  •  ".join(PackedStringArray(inventory.keys().map(func(k): return "%s %d" % [k, inventory[k]])))
	if stats.vita <= 0:
		status_label.text = "Sei svenuto: torna al menu e carica un salvataggio."

func _interact() -> void:
	var closest: Node3D
	var best := 2.8
	for node in get_children():
		if node is Area3D and node.has_meta("kind"):
			var d := player.global_position.distance_to(node.global_position)
			if d < best:
				best = d
				closest = node
	if closest:
		var kind: String = closest.get_meta("kind")
		match kind:
			"Albero":
				inventory["Legno"] += 3
				inventory["Fibra"] += 1
			"Roccia":
				inventory["Pietra"] += 2
			"Cespuglio":
				inventory["Bacche"] += 2
			"Rottami":
				inventory["Rottami"] += 2
		closest.queue_free()
		status_label.text = "Raccolta: " + kind
	else:
		status_label.text = "Nessuna risorsa vicina. Avvicinati e premi AZIONE."

func _build_selected() -> void:
	var cost := {"Pavimento":{"Legno":3}, "Muro":{"Legno":4}, "Porta":{"Legno":5,"Rottami":1}, "Finestra":{"Legno":3,"Rottami":2}, "Tetto":{"Legno":5}, "Fuoco":{"Legno":3,"Pietra":2}, "Armadio":{"Legno":6}, "Banco da lavoro":{"Legno":5,"Pietra":3}, "Letto":{"Legno":5,"Fibra":3}, "Fornace":{"Pietra":8,"Rottami":2}, "Rastrelliera":{"Legno":4,"Fibra":2}}
	var recipe: Dictionary = cost[build_choice]
	for item in recipe:
		if inventory.get(item, 0) < recipe[item]:
			status_label.text = "Risorse insufficienti per: " + build_choice
			return
	for item in recipe:
		inventory[item] -= recipe[item]
	var piece := _make_structure(build_choice, player.global_position + Vector3(0,0,-2.5))
	structures.append({"tipo":build_choice,"x":piece.position.x,"z":piece.position.z})
	status_label.text = build_choice + " costruito"

func _make_structure(kind: String, pos: Vector3) -> Node3D:
	var node := Node3D.new()
	node.position = Vector3(roundf(pos.x/2.0)*2.0, 0, roundf(pos.z/2.0)*2.0)
	var mesh_instance := MeshInstance3D.new()
	var mesh: Mesh = BoxMesh.new()
	var size := Vector3(2,2,0.25)
	match kind:
		"Muro": size = Vector3(2,2,0.22)
		"Porta": size = Vector3(1.5,2,0.18)
		"Finestra": size = Vector3(2,1.2,0.18)
		"Tetto": size = Vector3(2.2,0.2,2.2)
		"Fuoco":
			mesh = SphereMesh.new()
			(mesh as SphereMesh).radius = 0.55
			(mesh as SphereMesh).height = 0.7
			size = Vector3.ONE
		"Pavimento": size = Vector3(2,0.18,2)
		"Armadio": size = Vector3(1.1,1.8,0.65)
		"Banco da lavoro": size = Vector3(1.6,0.9,0.8)
		"Letto": size = Vector3(2,0.55,1)
		"Fornace": size = Vector3(1,1.3,1)
		"Rastrelliera": size = Vector3(1.6,1.8,0.35)
	if mesh is BoxMesh:
		(mesh as BoxMesh).size = size
	mesh_instance.mesh = mesh
	mesh_instance.position.y = size.y/2.0 if kind != "Tetto" else 2.2
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.38,0.24,0.13) if kind != "Fuoco" else Color(1.0,0.32,0.05)
	mat.roughness = 0.88
	mesh_instance.material_override = mat
	node.add_child(mesh_instance)
	add_child(node)
	return node

func _craft_axe() -> void:
	# Ricette rapide: premendo il pulsante si crea l'oggetto successivo disponibile.
	if inventory["Legno"] >= 2 and inventory["Pietra"] >= 2 and inventory["Fibra"] >= 1:
		inventory["Legno"] -= 2
		inventory["Pietra"] -= 2
		inventory["Fibra"] -= 1
		inventory["Rottami"] += 0
		status_label.text = "Ascia rudimentale creata: attrezzo da sopravvivenza."
	elif inventory["Legno"] >= 2 and inventory["Pietra"] >= 1 and inventory["Fibra"] >= 2:
		inventory["Legno"] -= 2
		inventory["Pietra"] -= 1
		inventory["Fibra"] -= 2
		inventory["Rottami"] += 1
		status_label.text = "Lancia rudimentale creata: difenditi dalle minacce."
	elif inventory["Legno"] >= 2 and inventory["Fibra"] >= 2:
		inventory["Legno"] -= 2
		inventory["Fibra"] -= 2
		inventory["Munizioni"] += 3
		status_label.text = "Hai preparato 3 dardi rudimentali."
	else:
		status_label.text = "Ricette: ascia (2 legni, 2 pietre, 1 fibra); lancia (2 legni, 1 pietra, 2 fibre)." 

func _consume() -> void:
	if inventory["Bacche"] > 0:
		inventory["Bacche"] -= 1
		stats["cibo"] = minf(100, stats["cibo"] + 18)
		status_label.text = "Hai mangiato bacche."
	elif inventory["Acqua"] > 0:
		inventory["Acqua"] -= 1
		stats["acqua"] = minf(100, stats["acqua"] + 25)
		status_label.text = "Hai bevuto acqua."
	else:
		status_label.text = "Non hai cibo o acqua nello zaino."

func _rest() -> void:
	stats["energia"] = minf(100, stats["energia"] + 30)
	stats["bisogno"] = minf(100, stats["bisogno"] + 3)
	status_label.text = "Ti sei riposato un momento. Attenzione alle minacce vicine."

func _show_inventory() -> void:
	status_label.text = "Zaino: " + str(inventory)

func save_game(automatic: bool) -> void:
	if not game_started:
		return
	var data := {"player":{"x":player.position.x,"z":player.position.z},"stats":stats,"inventory":inventory,"structures":structures,"character":selected_character,"elapsed":elapsed}
	var f := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(data))
		status_label.text = "Partita salvata automaticamente" if automatic else "Partita salvata"
		if save_dot:
			save_dot.color = Color(0.2,0.95,0.35,1)
	else:
		status_label.text = "Errore durante il salvataggio"

func load_game() -> void:
	if not FileAccess.file_exists(SAVE_PATH):
		status_label_if_menu("Nessun salvataggio trovato.")
		return
	var f := FileAccess.open(SAVE_PATH, FileAccess.READ)
	var parsed = JSON.parse_string(f.get_as_text())
	if typeof(parsed) != TYPE_DICTIONARY:
		status_label_if_menu("Salvataggio non valido.")
		return
	selected_character = parsed.get("character","Esploratore")
	stats = parsed.get("stats",stats)
	inventory = parsed.get("inventory",inventory)
	structures = parsed.get("structures",[])
	elapsed = parsed.get("elapsed",0.0)
	_create_world()
	player.position = Vector3(parsed.get("player",{}).get("x",0),0.05,parsed.get("player",{}).get("z",0))
	for s in structures:
		_make_structure(s.tipo, Vector3(s.x,0,s.z))
	menu_root.queue_free()
	_create_hud()
	game_started = true

func status_label_if_menu(message: String) -> void:
	for child in menu_root.get_children():
		if child is VBoxContainer:
			var l := Label.new()
			l.text = message
			child.add_child(l)
			return

func _input(event: InputEvent) -> void:
	if not game_started:
		return
	var screen_width := get_viewport().get_visible_rect().size.x
	if event is InputEventScreenTouch:
		if event.pressed:
			if event.position.x >= screen_width * 0.52:
				look_touch_id = event.index
				last_look_pos = event.position
			else:
				joystick_origin = event.position
				joystick_active = true
		else:
			if event.index == look_touch_id:
				look_touch_id = -1
			if event.position.x < screen_width * 0.52:
				joystick_active = false
				touch_vector = Vector2.ZERO
	elif event is InputEventScreenDrag:
		if event.index == look_touch_id:
			var drag_delta: Vector2 = event.position - last_look_pos
			camera_yaw -= drag_delta.x * 0.006
			camera_pitch = clampf(camera_pitch - drag_delta.y * 0.004, -1.25, -0.25)
			last_look_pos = event.position
			_update_camera_transform()
		elif joystick_active:
			var move_delta: Vector2 = event.position - joystick_origin
			touch_vector = Vector2(clampf(move_delta.x/55.0,-1,1), clampf(move_delta.y/55.0,-1,1))
	elif event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_RIGHT:
			mouse_looking = event.pressed
		elif event.button_index == MOUSE_BUTTON_WHEEL_UP and event.pressed:
			camera_distance = clampf(camera_distance - 1.2, 9.0, 27.0)
			_update_camera_transform()
		elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN and event.pressed:
			camera_distance = clampf(camera_distance + 1.2, 9.0, 27.0)
			_update_camera_transform()
	elif event is InputEventMouseMotion and mouse_looking:
		camera_yaw -= event.relative.x * 0.006
		camera_pitch = clampf(camera_pitch - event.relative.y * 0.004, -1.25, -0.25)
		_update_camera_transform()
