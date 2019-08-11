import sys

modid = 'quark'
params = {}

def copy_template(name, base, target):
	base_file = 'templates/{0}'.format(base)
	target_file = '../{0}'.format(target)

	with open(base_file, 'r') as reader:
		with open(target_file, 'w') as writer:
			for line in reader:
				line = line.replace('%modid%', modid)
				line = line.replace('%name%', name)
				for param in params:
					line = line.replace('%{0}%'.format(param), params[param])

				writer.write(line)

def copy(templates):
	foreach_arg(templates, copy_callback)

def copy_callback(name, templates):
	if '=' in name:
		parse_param(name)
	else:
		for tup in templates:
			base = tup[0]
			target = tup[1].format(modid = modid, name = name)
			copy_template(name, base, target)

def localize_standard(prefix):
	localize((
		lambda name, modid: '{prefix}.{modid}.{name}'.format(prefix = prefix, name = name, modid = modid),
		lambda name, modid: ' '.join(map(lambda s: s.capitalize(), name.split('_')))
	))

def localize(func):
	foreach_arg(func, localize_callback)

def localize_callback(name, funcs):
	key = funcs[0](name, modid)
	val = funcs[1](name, modid)
	print('"{0}": "{1}",'.format(key, val))

def foreach_arg(templates, func):
	if 'file:' in sys.argv[1]:
		foreach_arg_file(sys.argv[1][5:], templates, func)
	else:
		foreach_arg_array(1, sys.argv, templates, func)

def foreach_arg_file(file, templates, func):
	lines = []
	with open(file, 'r') as reader:
		for line in reader:
			lines.append(line.strip())

	foreach_arg_array(0, lines, templates, func)

def foreach_arg_array(start, arr, templates, func):
	argslen = len(arr)
	for i in range(start, argslen):
		name = arr[i]
		func(name, templates)

def parse_param(str):
	toks = str.split('=')
	params[toks[0]] = toks[1]