
?
inputPlaceholder*
shape:���������I*
dtype0
G
output_targetPlaceholder*
shape:���������*
dtype0
Z
%fully_connected0/weights/weight/shapeConst*
valueB"I   �   *
dtype0
Q
$fully_connected0/weights/weight/meanConst*
valueB
 *    *
dtype0
S
&fully_connected0/weights/weight/stddevConst*
valueB
 *���=*
dtype0
�
/fully_connected0/weights/weight/TruncatedNormalTruncatedNormal%fully_connected0/weights/weight/shape*

seed *
T0*
dtype0*
seed2 
�
#fully_connected0/weights/weight/mulMul/fully_connected0/weights/weight/TruncatedNormal&fully_connected0/weights/weight/stddev*
T0
z
fully_connected0/weights/weightAdd#fully_connected0/weights/weight/mul$fully_connected0/weights/weight/mean*
T0
v
!fully_connected0/weights/Variable
VariableV2*
dtype0*
	container *
shape:	I�*
shared_name 
�
(fully_connected0/weights/Variable/AssignAssign!fully_connected0/weights/Variablefully_connected0/weights/weight*
use_locking(*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable*
validate_shape(
�
&fully_connected0/weights/Variable/readIdentity!fully_connected0/weights/Variable*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
N
fully_connected0/biases/biasConst*
valueB�*���=*
dtype0
q
 fully_connected0/biases/Variable
VariableV2*
shape:�*
shared_name *
dtype0*
	container 
�
'fully_connected0/biases/Variable/AssignAssign fully_connected0/biases/Variablefully_connected0/biases/bias*
validate_shape(*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
%fully_connected0/biases/Variable/readIdentity fully_connected0/biases/Variable*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
!fully_connected0/Wx_plus_b/MatMulMatMulinput&fully_connected0/weights/Variable/read*
T0*
transpose_a( *
transpose_b( 
x
fully_connected0/Wx_plus_b/addAdd!fully_connected0/Wx_plus_b/MatMul%fully_connected0/biases/Variable/read*
T0
a
fully_connected0/activation	LeakyRelufully_connected0/Wx_plus_b/add*
T0*
alpha%��L>
Z
%fully_connected1/weights/weight/shapeConst*
valueB"�       *
dtype0
Q
$fully_connected1/weights/weight/meanConst*
valueB
 *    *
dtype0
S
&fully_connected1/weights/weight/stddevConst*
valueB
 *���=*
dtype0
�
/fully_connected1/weights/weight/TruncatedNormalTruncatedNormal%fully_connected1/weights/weight/shape*
T0*
dtype0*
seed2 *

seed 
�
#fully_connected1/weights/weight/mulMul/fully_connected1/weights/weight/TruncatedNormal&fully_connected1/weights/weight/stddev*
T0
z
fully_connected1/weights/weightAdd#fully_connected1/weights/weight/mul$fully_connected1/weights/weight/mean*
T0
v
!fully_connected1/weights/Variable
VariableV2*
shared_name *
dtype0*
	container *
shape:	� 
�
(fully_connected1/weights/Variable/AssignAssign!fully_connected1/weights/Variablefully_connected1/weights/weight*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable
�
&fully_connected1/weights/Variable/readIdentity!fully_connected1/weights/Variable*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable
M
fully_connected1/biases/biasConst*
valueB *���=*
dtype0
p
 fully_connected1/biases/Variable
VariableV2*
shared_name *
dtype0*
	container *
shape: 
�
'fully_connected1/biases/Variable/AssignAssign fully_connected1/biases/Variablefully_connected1/biases/bias*
validate_shape(*
use_locking(*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable
�
%fully_connected1/biases/Variable/readIdentity fully_connected1/biases/Variable*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable
�
!fully_connected1/Wx_plus_b/MatMulMatMulfully_connected0/activation&fully_connected1/weights/Variable/read*
transpose_a( *
transpose_b( *
T0
x
fully_connected1/Wx_plus_b/addAdd!fully_connected1/Wx_plus_b/MatMul%fully_connected1/biases/Variable/read*
T0
a
fully_connected1/activation	LeakyRelufully_connected1/Wx_plus_b/add*
T0*
alpha%��L>
Z
%fully_connected2/weights/weight/shapeConst*
valueB"       *
dtype0
Q
$fully_connected2/weights/weight/meanConst*
valueB
 *    *
dtype0
S
&fully_connected2/weights/weight/stddevConst*
valueB
 *���=*
dtype0
�
/fully_connected2/weights/weight/TruncatedNormalTruncatedNormal%fully_connected2/weights/weight/shape*
T0*
dtype0*
seed2 *

seed 
�
#fully_connected2/weights/weight/mulMul/fully_connected2/weights/weight/TruncatedNormal&fully_connected2/weights/weight/stddev*
T0
z
fully_connected2/weights/weightAdd#fully_connected2/weights/weight/mul$fully_connected2/weights/weight/mean*
T0
u
!fully_connected2/weights/Variable
VariableV2*
dtype0*
	container *
shape
: *
shared_name 
�
(fully_connected2/weights/Variable/AssignAssign!fully_connected2/weights/Variablefully_connected2/weights/weight*4
_class*
(&loc:@fully_connected2/weights/Variable*
validate_shape(*
use_locking(*
T0
�
&fully_connected2/weights/Variable/readIdentity!fully_connected2/weights/Variable*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable
M
fully_connected2/biases/biasConst*
valueB*���=*
dtype0
p
 fully_connected2/biases/Variable
VariableV2*
dtype0*
	container *
shape:*
shared_name 
�
'fully_connected2/biases/Variable/AssignAssign fully_connected2/biases/Variablefully_connected2/biases/bias*3
_class)
'%loc:@fully_connected2/biases/Variable*
validate_shape(*
use_locking(*
T0
�
%fully_connected2/biases/Variable/readIdentity fully_connected2/biases/Variable*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable
�
!fully_connected2/Wx_plus_b/MatMulMatMulfully_connected1/activation&fully_connected2/weights/Variable/read*
transpose_a( *
transpose_b( *
T0
x
fully_connected2/Wx_plus_b/addAdd!fully_connected2/Wx_plus_b/MatMul%fully_connected2/biases/Variable/read*
T0
a
fully_connected2/activation	LeakyRelufully_connected2/Wx_plus_b/add*
T0*
alpha%��L>
C
output_activationIdentityfully_connected2/activation*
T0
@
huber_loss/SubSuboutput_activationoutput_target*
T0
.
huber_loss/AbsAbshuber_loss/Sub*
T0
A
huber_loss/Minimum/yConst*
valueB
 *  �?*
dtype0
L
huber_loss/MinimumMinimumhuber_loss/Abshuber_loss/Minimum/y*
T0
D
huber_loss/Sub_1Subhuber_loss/Abshuber_loss/Minimum*
T0
=
huber_loss/ConstConst*
valueB
 *   ?*
dtype0
F
huber_loss/MulMulhuber_loss/Minimumhuber_loss/Minimum*
T0
B
huber_loss/Mul_1Mulhuber_loss/Consthuber_loss/Mul*
T0
?
huber_loss/Mul_2/xConst*
valueB
 *  �?*
dtype0
F
huber_loss/Mul_2Mulhuber_loss/Mul_2/xhuber_loss/Sub_1*
T0
B
huber_loss/AddAddhuber_loss/Mul_1huber_loss/Mul_2*
T0
T
'huber_loss/assert_broadcastable/weightsConst*
valueB
 *  �?*
dtype0
V
-huber_loss/assert_broadcastable/weights/shapeConst*
valueB *
dtype0
V
,huber_loss/assert_broadcastable/weights/rankConst*
value	B : *
dtype0
^
,huber_loss/assert_broadcastable/values/shapeShapehuber_loss/Add*
T0*
out_type0
U
+huber_loss/assert_broadcastable/values/rankConst*
value	B :*
dtype0
C
;huber_loss/assert_broadcastable/static_scalar_check_successNoOp

huber_loss/ToFloat/xConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
valueB
 *  �?*
dtype0
F
huber_loss/Mul_3Mulhuber_loss/Addhuber_loss/ToFloat/x*
T0
�
huber_loss/Const_1Const<^huber_loss/assert_broadcastable/static_scalar_check_success*
dtype0*
valueB"       
a
huber_loss/SumSumhuber_loss/Mul_3huber_loss/Const_1*
T0*

Tidx0*
	keep_dims( 
�
huber_loss/num_present/Equal/yConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
valueB
 *    *
dtype0
d
huber_loss/num_present/EqualEqualhuber_loss/ToFloat/xhuber_loss/num_present/Equal/y*
T0
�
!huber_loss/num_present/zeros_likeConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
valueB
 *    *
dtype0
�
&huber_loss/num_present/ones_like/ShapeConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
valueB *
dtype0
�
&huber_loss/num_present/ones_like/ConstConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
dtype0*
valueB
 *  �?
�
 huber_loss/num_present/ones_likeFill&huber_loss/num_present/ones_like/Shape&huber_loss/num_present/ones_like/Const*
T0*

index_type0
�
huber_loss/num_present/SelectSelecthuber_loss/num_present/Equal!huber_loss/num_present/zeros_like huber_loss/num_present/ones_like*
T0
�
Khuber_loss/num_present/broadcast_weights/assert_broadcastable/weights/shapeConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
valueB *
dtype0
�
Jhuber_loss/num_present/broadcast_weights/assert_broadcastable/weights/rankConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
value	B : *
dtype0
�
Jhuber_loss/num_present/broadcast_weights/assert_broadcastable/values/shapeShapehuber_loss/Add<^huber_loss/assert_broadcastable/static_scalar_check_success*
T0*
out_type0
�
Ihuber_loss/num_present/broadcast_weights/assert_broadcastable/values/rankConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
value	B :*
dtype0
�
Yhuber_loss/num_present/broadcast_weights/assert_broadcastable/static_scalar_check_successNoOp<^huber_loss/assert_broadcastable/static_scalar_check_success
�
8huber_loss/num_present/broadcast_weights/ones_like/ShapeShapehuber_loss/Add<^huber_loss/assert_broadcastable/static_scalar_check_successZ^huber_loss/num_present/broadcast_weights/assert_broadcastable/static_scalar_check_success*
T0*
out_type0
�
8huber_loss/num_present/broadcast_weights/ones_like/ConstConst<^huber_loss/assert_broadcastable/static_scalar_check_successZ^huber_loss/num_present/broadcast_weights/assert_broadcastable/static_scalar_check_success*
dtype0*
valueB
 *  �?
�
2huber_loss/num_present/broadcast_weights/ones_likeFill8huber_loss/num_present/broadcast_weights/ones_like/Shape8huber_loss/num_present/broadcast_weights/ones_like/Const*
T0*

index_type0
�
(huber_loss/num_present/broadcast_weightsMulhuber_loss/num_present/Select2huber_loss/num_present/broadcast_weights/ones_like*
T0
�
huber_loss/num_present/ConstConst<^huber_loss/assert_broadcastable/static_scalar_check_success*
valueB"       *
dtype0
�
huber_loss/num_presentSum(huber_loss/num_present/broadcast_weightshuber_loss/num_present/Const*

Tidx0*
	keep_dims( *
T0
y
huber_loss/Const_2Const<^huber_loss/assert_broadcastable/static_scalar_check_success*
dtype0*
valueB 
a
huber_loss/Sum_1Sumhuber_loss/Sumhuber_loss/Const_2*

Tidx0*
	keep_dims( *
T0
O
huber_loss/valueDivNoNanhuber_loss/Sum_1huber_loss/num_present*
T0
+
lossIdentityhuber_loss/value*
T0
.
outputIdentityoutput_activation*
T0
8
gradients/ShapeConst*
valueB *
dtype0
@
gradients/grad_ys_0Const*
dtype0*
valueB
 *  �?
W
gradients/FillFillgradients/Shapegradients/grad_ys_0*
T0*

index_type0
N
%gradients/huber_loss/value_grad/ShapeConst*
valueB *
dtype0
P
'gradients/huber_loss/value_grad/Shape_1Const*
valueB *
dtype0
�
5gradients/huber_loss/value_grad/BroadcastGradientArgsBroadcastGradientArgs%gradients/huber_loss/value_grad/Shape'gradients/huber_loss/value_grad/Shape_1*
T0
g
*gradients/huber_loss/value_grad/div_no_nanDivNoNangradients/Fillhuber_loss/num_present*
T0
�
#gradients/huber_loss/value_grad/SumSum*gradients/huber_loss/value_grad/div_no_nan5gradients/huber_loss/value_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/value_grad/ReshapeReshape#gradients/huber_loss/value_grad/Sum%gradients/huber_loss/value_grad/Shape*
T0*
Tshape0
E
#gradients/huber_loss/value_grad/NegNeghuber_loss/Sum_1*
T0
~
,gradients/huber_loss/value_grad/div_no_nan_1DivNoNan#gradients/huber_loss/value_grad/Neghuber_loss/num_present*
T0
�
,gradients/huber_loss/value_grad/div_no_nan_2DivNoNan,gradients/huber_loss/value_grad/div_no_nan_1huber_loss/num_present*
T0
q
#gradients/huber_loss/value_grad/mulMulgradients/Fill,gradients/huber_loss/value_grad/div_no_nan_2*
T0
�
%gradients/huber_loss/value_grad/Sum_1Sum#gradients/huber_loss/value_grad/mul7gradients/huber_loss/value_grad/BroadcastGradientArgs:1*
T0*

Tidx0*
	keep_dims( 
�
)gradients/huber_loss/value_grad/Reshape_1Reshape%gradients/huber_loss/value_grad/Sum_1'gradients/huber_loss/value_grad/Shape_1*
T0*
Tshape0
�
0gradients/huber_loss/value_grad/tuple/group_depsNoOp(^gradients/huber_loss/value_grad/Reshape*^gradients/huber_loss/value_grad/Reshape_1
�
8gradients/huber_loss/value_grad/tuple/control_dependencyIdentity'gradients/huber_loss/value_grad/Reshape1^gradients/huber_loss/value_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/value_grad/Reshape
�
:gradients/huber_loss/value_grad/tuple/control_dependency_1Identity)gradients/huber_loss/value_grad/Reshape_11^gradients/huber_loss/value_grad/tuple/group_deps*<
_class2
0.loc:@gradients/huber_loss/value_grad/Reshape_1*
T0
V
-gradients/huber_loss/Sum_1_grad/Reshape/shapeConst*
valueB *
dtype0
�
'gradients/huber_loss/Sum_1_grad/ReshapeReshape8gradients/huber_loss/value_grad/tuple/control_dependency-gradients/huber_loss/Sum_1_grad/Reshape/shape*
T0*
Tshape0
N
%gradients/huber_loss/Sum_1_grad/ConstConst*
valueB *
dtype0
�
$gradients/huber_loss/Sum_1_grad/TileTile'gradients/huber_loss/Sum_1_grad/Reshape%gradients/huber_loss/Sum_1_grad/Const*

Tmultiples0*
T0
`
+gradients/huber_loss/Sum_grad/Reshape/shapeConst*
valueB"      *
dtype0
�
%gradients/huber_loss/Sum_grad/ReshapeReshape$gradients/huber_loss/Sum_1_grad/Tile+gradients/huber_loss/Sum_grad/Reshape/shape*
T0*
Tshape0
W
#gradients/huber_loss/Sum_grad/ShapeShapehuber_loss/Mul_3*
out_type0*
T0
�
"gradients/huber_loss/Sum_grad/TileTile%gradients/huber_loss/Sum_grad/Reshape#gradients/huber_loss/Sum_grad/Shape*

Tmultiples0*
T0
W
%gradients/huber_loss/Mul_3_grad/ShapeShapehuber_loss/Add*
out_type0*
T0
P
'gradients/huber_loss/Mul_3_grad/Shape_1Const*
valueB *
dtype0
�
5gradients/huber_loss/Mul_3_grad/BroadcastGradientArgsBroadcastGradientArgs%gradients/huber_loss/Mul_3_grad/Shape'gradients/huber_loss/Mul_3_grad/Shape_1*
T0
m
#gradients/huber_loss/Mul_3_grad/MulMul"gradients/huber_loss/Sum_grad/Tilehuber_loss/ToFloat/x*
T0
�
#gradients/huber_loss/Mul_3_grad/SumSum#gradients/huber_loss/Mul_3_grad/Mul5gradients/huber_loss/Mul_3_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/Mul_3_grad/ReshapeReshape#gradients/huber_loss/Mul_3_grad/Sum%gradients/huber_loss/Mul_3_grad/Shape*
T0*
Tshape0
i
%gradients/huber_loss/Mul_3_grad/Mul_1Mulhuber_loss/Add"gradients/huber_loss/Sum_grad/Tile*
T0
�
%gradients/huber_loss/Mul_3_grad/Sum_1Sum%gradients/huber_loss/Mul_3_grad/Mul_17gradients/huber_loss/Mul_3_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
)gradients/huber_loss/Mul_3_grad/Reshape_1Reshape%gradients/huber_loss/Mul_3_grad/Sum_1'gradients/huber_loss/Mul_3_grad/Shape_1*
Tshape0*
T0
�
0gradients/huber_loss/Mul_3_grad/tuple/group_depsNoOp(^gradients/huber_loss/Mul_3_grad/Reshape*^gradients/huber_loss/Mul_3_grad/Reshape_1
�
8gradients/huber_loss/Mul_3_grad/tuple/control_dependencyIdentity'gradients/huber_loss/Mul_3_grad/Reshape1^gradients/huber_loss/Mul_3_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Mul_3_grad/Reshape
�
:gradients/huber_loss/Mul_3_grad/tuple/control_dependency_1Identity)gradients/huber_loss/Mul_3_grad/Reshape_11^gradients/huber_loss/Mul_3_grad/tuple/group_deps*<
_class2
0.loc:@gradients/huber_loss/Mul_3_grad/Reshape_1*
T0
W
#gradients/huber_loss/Add_grad/ShapeShapehuber_loss/Mul_1*
out_type0*
T0
Y
%gradients/huber_loss/Add_grad/Shape_1Shapehuber_loss/Mul_2*
T0*
out_type0
�
3gradients/huber_loss/Add_grad/BroadcastGradientArgsBroadcastGradientArgs#gradients/huber_loss/Add_grad/Shape%gradients/huber_loss/Add_grad/Shape_1*
T0
�
!gradients/huber_loss/Add_grad/SumSum8gradients/huber_loss/Mul_3_grad/tuple/control_dependency3gradients/huber_loss/Add_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
%gradients/huber_loss/Add_grad/ReshapeReshape!gradients/huber_loss/Add_grad/Sum#gradients/huber_loss/Add_grad/Shape*
T0*
Tshape0
�
#gradients/huber_loss/Add_grad/Sum_1Sum8gradients/huber_loss/Mul_3_grad/tuple/control_dependency5gradients/huber_loss/Add_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/Add_grad/Reshape_1Reshape#gradients/huber_loss/Add_grad/Sum_1%gradients/huber_loss/Add_grad/Shape_1*
T0*
Tshape0
�
.gradients/huber_loss/Add_grad/tuple/group_depsNoOp&^gradients/huber_loss/Add_grad/Reshape(^gradients/huber_loss/Add_grad/Reshape_1
�
6gradients/huber_loss/Add_grad/tuple/control_dependencyIdentity%gradients/huber_loss/Add_grad/Reshape/^gradients/huber_loss/Add_grad/tuple/group_deps*
T0*8
_class.
,*loc:@gradients/huber_loss/Add_grad/Reshape
�
8gradients/huber_loss/Add_grad/tuple/control_dependency_1Identity'gradients/huber_loss/Add_grad/Reshape_1/^gradients/huber_loss/Add_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Add_grad/Reshape_1
N
%gradients/huber_loss/Mul_1_grad/ShapeConst*
valueB *
dtype0
Y
'gradients/huber_loss/Mul_1_grad/Shape_1Shapehuber_loss/Mul*
T0*
out_type0
�
5gradients/huber_loss/Mul_1_grad/BroadcastGradientArgsBroadcastGradientArgs%gradients/huber_loss/Mul_1_grad/Shape'gradients/huber_loss/Mul_1_grad/Shape_1*
T0
{
#gradients/huber_loss/Mul_1_grad/MulMul6gradients/huber_loss/Add_grad/tuple/control_dependencyhuber_loss/Mul*
T0
�
#gradients/huber_loss/Mul_1_grad/SumSum#gradients/huber_loss/Mul_1_grad/Mul5gradients/huber_loss/Mul_1_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/Mul_1_grad/ReshapeReshape#gradients/huber_loss/Mul_1_grad/Sum%gradients/huber_loss/Mul_1_grad/Shape*
T0*
Tshape0

%gradients/huber_loss/Mul_1_grad/Mul_1Mulhuber_loss/Const6gradients/huber_loss/Add_grad/tuple/control_dependency*
T0
�
%gradients/huber_loss/Mul_1_grad/Sum_1Sum%gradients/huber_loss/Mul_1_grad/Mul_17gradients/huber_loss/Mul_1_grad/BroadcastGradientArgs:1*
T0*

Tidx0*
	keep_dims( 
�
)gradients/huber_loss/Mul_1_grad/Reshape_1Reshape%gradients/huber_loss/Mul_1_grad/Sum_1'gradients/huber_loss/Mul_1_grad/Shape_1*
T0*
Tshape0
�
0gradients/huber_loss/Mul_1_grad/tuple/group_depsNoOp(^gradients/huber_loss/Mul_1_grad/Reshape*^gradients/huber_loss/Mul_1_grad/Reshape_1
�
8gradients/huber_loss/Mul_1_grad/tuple/control_dependencyIdentity'gradients/huber_loss/Mul_1_grad/Reshape1^gradients/huber_loss/Mul_1_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Mul_1_grad/Reshape
�
:gradients/huber_loss/Mul_1_grad/tuple/control_dependency_1Identity)gradients/huber_loss/Mul_1_grad/Reshape_11^gradients/huber_loss/Mul_1_grad/tuple/group_deps*<
_class2
0.loc:@gradients/huber_loss/Mul_1_grad/Reshape_1*
T0
N
%gradients/huber_loss/Mul_2_grad/ShapeConst*
valueB *
dtype0
[
'gradients/huber_loss/Mul_2_grad/Shape_1Shapehuber_loss/Sub_1*
T0*
out_type0
�
5gradients/huber_loss/Mul_2_grad/BroadcastGradientArgsBroadcastGradientArgs%gradients/huber_loss/Mul_2_grad/Shape'gradients/huber_loss/Mul_2_grad/Shape_1*
T0

#gradients/huber_loss/Mul_2_grad/MulMul8gradients/huber_loss/Add_grad/tuple/control_dependency_1huber_loss/Sub_1*
T0
�
#gradients/huber_loss/Mul_2_grad/SumSum#gradients/huber_loss/Mul_2_grad/Mul5gradients/huber_loss/Mul_2_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/Mul_2_grad/ReshapeReshape#gradients/huber_loss/Mul_2_grad/Sum%gradients/huber_loss/Mul_2_grad/Shape*
T0*
Tshape0
�
%gradients/huber_loss/Mul_2_grad/Mul_1Mulhuber_loss/Mul_2/x8gradients/huber_loss/Add_grad/tuple/control_dependency_1*
T0
�
%gradients/huber_loss/Mul_2_grad/Sum_1Sum%gradients/huber_loss/Mul_2_grad/Mul_17gradients/huber_loss/Mul_2_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
)gradients/huber_loss/Mul_2_grad/Reshape_1Reshape%gradients/huber_loss/Mul_2_grad/Sum_1'gradients/huber_loss/Mul_2_grad/Shape_1*
T0*
Tshape0
�
0gradients/huber_loss/Mul_2_grad/tuple/group_depsNoOp(^gradients/huber_loss/Mul_2_grad/Reshape*^gradients/huber_loss/Mul_2_grad/Reshape_1
�
8gradients/huber_loss/Mul_2_grad/tuple/control_dependencyIdentity'gradients/huber_loss/Mul_2_grad/Reshape1^gradients/huber_loss/Mul_2_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Mul_2_grad/Reshape
�
:gradients/huber_loss/Mul_2_grad/tuple/control_dependency_1Identity)gradients/huber_loss/Mul_2_grad/Reshape_11^gradients/huber_loss/Mul_2_grad/tuple/group_deps*
T0*<
_class2
0.loc:@gradients/huber_loss/Mul_2_grad/Reshape_1
Y
#gradients/huber_loss/Mul_grad/ShapeShapehuber_loss/Minimum*
T0*
out_type0
[
%gradients/huber_loss/Mul_grad/Shape_1Shapehuber_loss/Minimum*
T0*
out_type0
�
3gradients/huber_loss/Mul_grad/BroadcastGradientArgsBroadcastGradientArgs#gradients/huber_loss/Mul_grad/Shape%gradients/huber_loss/Mul_grad/Shape_1*
T0
�
!gradients/huber_loss/Mul_grad/MulMul:gradients/huber_loss/Mul_1_grad/tuple/control_dependency_1huber_loss/Minimum*
T0
�
!gradients/huber_loss/Mul_grad/SumSum!gradients/huber_loss/Mul_grad/Mul3gradients/huber_loss/Mul_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
%gradients/huber_loss/Mul_grad/ReshapeReshape!gradients/huber_loss/Mul_grad/Sum#gradients/huber_loss/Mul_grad/Shape*
T0*
Tshape0
�
#gradients/huber_loss/Mul_grad/Mul_1Mulhuber_loss/Minimum:gradients/huber_loss/Mul_1_grad/tuple/control_dependency_1*
T0
�
#gradients/huber_loss/Mul_grad/Sum_1Sum#gradients/huber_loss/Mul_grad/Mul_15gradients/huber_loss/Mul_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/Mul_grad/Reshape_1Reshape#gradients/huber_loss/Mul_grad/Sum_1%gradients/huber_loss/Mul_grad/Shape_1*
T0*
Tshape0
�
.gradients/huber_loss/Mul_grad/tuple/group_depsNoOp&^gradients/huber_loss/Mul_grad/Reshape(^gradients/huber_loss/Mul_grad/Reshape_1
�
6gradients/huber_loss/Mul_grad/tuple/control_dependencyIdentity%gradients/huber_loss/Mul_grad/Reshape/^gradients/huber_loss/Mul_grad/tuple/group_deps*
T0*8
_class.
,*loc:@gradients/huber_loss/Mul_grad/Reshape
�
8gradients/huber_loss/Mul_grad/tuple/control_dependency_1Identity'gradients/huber_loss/Mul_grad/Reshape_1/^gradients/huber_loss/Mul_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Mul_grad/Reshape_1
W
%gradients/huber_loss/Sub_1_grad/ShapeShapehuber_loss/Abs*
T0*
out_type0
]
'gradients/huber_loss/Sub_1_grad/Shape_1Shapehuber_loss/Minimum*
T0*
out_type0
�
5gradients/huber_loss/Sub_1_grad/BroadcastGradientArgsBroadcastGradientArgs%gradients/huber_loss/Sub_1_grad/Shape'gradients/huber_loss/Sub_1_grad/Shape_1*
T0
�
#gradients/huber_loss/Sub_1_grad/SumSum:gradients/huber_loss/Mul_2_grad/tuple/control_dependency_15gradients/huber_loss/Sub_1_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
'gradients/huber_loss/Sub_1_grad/ReshapeReshape#gradients/huber_loss/Sub_1_grad/Sum%gradients/huber_loss/Sub_1_grad/Shape*
T0*
Tshape0
�
%gradients/huber_loss/Sub_1_grad/Sum_1Sum:gradients/huber_loss/Mul_2_grad/tuple/control_dependency_17gradients/huber_loss/Sub_1_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
Z
#gradients/huber_loss/Sub_1_grad/NegNeg%gradients/huber_loss/Sub_1_grad/Sum_1*
T0
�
)gradients/huber_loss/Sub_1_grad/Reshape_1Reshape#gradients/huber_loss/Sub_1_grad/Neg'gradients/huber_loss/Sub_1_grad/Shape_1*
T0*
Tshape0
�
0gradients/huber_loss/Sub_1_grad/tuple/group_depsNoOp(^gradients/huber_loss/Sub_1_grad/Reshape*^gradients/huber_loss/Sub_1_grad/Reshape_1
�
8gradients/huber_loss/Sub_1_grad/tuple/control_dependencyIdentity'gradients/huber_loss/Sub_1_grad/Reshape1^gradients/huber_loss/Sub_1_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Sub_1_grad/Reshape
�
:gradients/huber_loss/Sub_1_grad/tuple/control_dependency_1Identity)gradients/huber_loss/Sub_1_grad/Reshape_11^gradients/huber_loss/Sub_1_grad/tuple/group_deps*
T0*<
_class2
0.loc:@gradients/huber_loss/Sub_1_grad/Reshape_1
�
gradients/AddNAddN6gradients/huber_loss/Mul_grad/tuple/control_dependency8gradients/huber_loss/Mul_grad/tuple/control_dependency_1:gradients/huber_loss/Sub_1_grad/tuple/control_dependency_1*
T0*8
_class.
,*loc:@gradients/huber_loss/Mul_grad/Reshape*
N
Y
'gradients/huber_loss/Minimum_grad/ShapeShapehuber_loss/Abs*
out_type0*
T0
R
)gradients/huber_loss/Minimum_grad/Shape_1Const*
valueB *
dtype0
[
)gradients/huber_loss/Minimum_grad/Shape_2Shapegradients/AddN*
T0*
out_type0
Z
-gradients/huber_loss/Minimum_grad/zeros/ConstConst*
valueB
 *    *
dtype0
�
'gradients/huber_loss/Minimum_grad/zerosFill)gradients/huber_loss/Minimum_grad/Shape_2-gradients/huber_loss/Minimum_grad/zeros/Const*
T0*

index_type0
g
+gradients/huber_loss/Minimum_grad/LessEqual	LessEqualhuber_loss/Abshuber_loss/Minimum/y*
T0
�
7gradients/huber_loss/Minimum_grad/BroadcastGradientArgsBroadcastGradientArgs'gradients/huber_loss/Minimum_grad/Shape)gradients/huber_loss/Minimum_grad/Shape_1*
T0
�
(gradients/huber_loss/Minimum_grad/SelectSelect+gradients/huber_loss/Minimum_grad/LessEqualgradients/AddN'gradients/huber_loss/Minimum_grad/zeros*
T0
�
*gradients/huber_loss/Minimum_grad/Select_1Select+gradients/huber_loss/Minimum_grad/LessEqual'gradients/huber_loss/Minimum_grad/zerosgradients/AddN*
T0
�
%gradients/huber_loss/Minimum_grad/SumSum(gradients/huber_loss/Minimum_grad/Select7gradients/huber_loss/Minimum_grad/BroadcastGradientArgs*
T0*

Tidx0*
	keep_dims( 
�
)gradients/huber_loss/Minimum_grad/ReshapeReshape%gradients/huber_loss/Minimum_grad/Sum'gradients/huber_loss/Minimum_grad/Shape*
T0*
Tshape0
�
'gradients/huber_loss/Minimum_grad/Sum_1Sum*gradients/huber_loss/Minimum_grad/Select_19gradients/huber_loss/Minimum_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
+gradients/huber_loss/Minimum_grad/Reshape_1Reshape'gradients/huber_loss/Minimum_grad/Sum_1)gradients/huber_loss/Minimum_grad/Shape_1*
T0*
Tshape0
�
2gradients/huber_loss/Minimum_grad/tuple/group_depsNoOp*^gradients/huber_loss/Minimum_grad/Reshape,^gradients/huber_loss/Minimum_grad/Reshape_1
�
:gradients/huber_loss/Minimum_grad/tuple/control_dependencyIdentity)gradients/huber_loss/Minimum_grad/Reshape3^gradients/huber_loss/Minimum_grad/tuple/group_deps*<
_class2
0.loc:@gradients/huber_loss/Minimum_grad/Reshape*
T0
�
<gradients/huber_loss/Minimum_grad/tuple/control_dependency_1Identity+gradients/huber_loss/Minimum_grad/Reshape_13^gradients/huber_loss/Minimum_grad/tuple/group_deps*
T0*>
_class4
20loc:@gradients/huber_loss/Minimum_grad/Reshape_1
�
gradients/AddN_1AddN8gradients/huber_loss/Sub_1_grad/tuple/control_dependency:gradients/huber_loss/Minimum_grad/tuple/control_dependency*
T0*:
_class0
.,loc:@gradients/huber_loss/Sub_1_grad/Reshape*
N
C
"gradients/huber_loss/Abs_grad/SignSignhuber_loss/Sub*
T0
g
!gradients/huber_loss/Abs_grad/mulMulgradients/AddN_1"gradients/huber_loss/Abs_grad/Sign*
T0
X
#gradients/huber_loss/Sub_grad/ShapeShapeoutput_activation*
T0*
out_type0
V
%gradients/huber_loss/Sub_grad/Shape_1Shapeoutput_target*
T0*
out_type0
�
3gradients/huber_loss/Sub_grad/BroadcastGradientArgsBroadcastGradientArgs#gradients/huber_loss/Sub_grad/Shape%gradients/huber_loss/Sub_grad/Shape_1*
T0
�
!gradients/huber_loss/Sub_grad/SumSum!gradients/huber_loss/Abs_grad/mul3gradients/huber_loss/Sub_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
%gradients/huber_loss/Sub_grad/ReshapeReshape!gradients/huber_loss/Sub_grad/Sum#gradients/huber_loss/Sub_grad/Shape*
T0*
Tshape0
�
#gradients/huber_loss/Sub_grad/Sum_1Sum!gradients/huber_loss/Abs_grad/mul5gradients/huber_loss/Sub_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
V
!gradients/huber_loss/Sub_grad/NegNeg#gradients/huber_loss/Sub_grad/Sum_1*
T0
�
'gradients/huber_loss/Sub_grad/Reshape_1Reshape!gradients/huber_loss/Sub_grad/Neg%gradients/huber_loss/Sub_grad/Shape_1*
T0*
Tshape0
�
.gradients/huber_loss/Sub_grad/tuple/group_depsNoOp&^gradients/huber_loss/Sub_grad/Reshape(^gradients/huber_loss/Sub_grad/Reshape_1
�
6gradients/huber_loss/Sub_grad/tuple/control_dependencyIdentity%gradients/huber_loss/Sub_grad/Reshape/^gradients/huber_loss/Sub_grad/tuple/group_deps*
T0*8
_class.
,*loc:@gradients/huber_loss/Sub_grad/Reshape
�
8gradients/huber_loss/Sub_grad/tuple/control_dependency_1Identity'gradients/huber_loss/Sub_grad/Reshape_1/^gradients/huber_loss/Sub_grad/tuple/group_deps*
T0*:
_class0
.,loc:@gradients/huber_loss/Sub_grad/Reshape_1
�
8gradients/fully_connected2/activation_grad/LeakyReluGradLeakyReluGrad6gradients/huber_loss/Sub_grad/tuple/control_dependencyfully_connected2/Wx_plus_b/add*
T0*
alpha%��L>
x
3gradients/fully_connected2/Wx_plus_b/add_grad/ShapeShape!fully_connected2/Wx_plus_b/MatMul*
T0*
out_type0
c
5gradients/fully_connected2/Wx_plus_b/add_grad/Shape_1Const*
dtype0*
valueB:
�
Cgradients/fully_connected2/Wx_plus_b/add_grad/BroadcastGradientArgsBroadcastGradientArgs3gradients/fully_connected2/Wx_plus_b/add_grad/Shape5gradients/fully_connected2/Wx_plus_b/add_grad/Shape_1*
T0
�
1gradients/fully_connected2/Wx_plus_b/add_grad/SumSum8gradients/fully_connected2/activation_grad/LeakyReluGradCgradients/fully_connected2/Wx_plus_b/add_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
5gradients/fully_connected2/Wx_plus_b/add_grad/ReshapeReshape1gradients/fully_connected2/Wx_plus_b/add_grad/Sum3gradients/fully_connected2/Wx_plus_b/add_grad/Shape*
T0*
Tshape0
�
3gradients/fully_connected2/Wx_plus_b/add_grad/Sum_1Sum8gradients/fully_connected2/activation_grad/LeakyReluGradEgradients/fully_connected2/Wx_plus_b/add_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
7gradients/fully_connected2/Wx_plus_b/add_grad/Reshape_1Reshape3gradients/fully_connected2/Wx_plus_b/add_grad/Sum_15gradients/fully_connected2/Wx_plus_b/add_grad/Shape_1*
T0*
Tshape0
�
>gradients/fully_connected2/Wx_plus_b/add_grad/tuple/group_depsNoOp6^gradients/fully_connected2/Wx_plus_b/add_grad/Reshape8^gradients/fully_connected2/Wx_plus_b/add_grad/Reshape_1
�
Fgradients/fully_connected2/Wx_plus_b/add_grad/tuple/control_dependencyIdentity5gradients/fully_connected2/Wx_plus_b/add_grad/Reshape?^gradients/fully_connected2/Wx_plus_b/add_grad/tuple/group_deps*
T0*H
_class>
<:loc:@gradients/fully_connected2/Wx_plus_b/add_grad/Reshape
�
Hgradients/fully_connected2/Wx_plus_b/add_grad/tuple/control_dependency_1Identity7gradients/fully_connected2/Wx_plus_b/add_grad/Reshape_1?^gradients/fully_connected2/Wx_plus_b/add_grad/tuple/group_deps*
T0*J
_class@
><loc:@gradients/fully_connected2/Wx_plus_b/add_grad/Reshape_1
�
7gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMulMatMulFgradients/fully_connected2/Wx_plus_b/add_grad/tuple/control_dependency&fully_connected2/weights/Variable/read*
transpose_a( *
transpose_b(*
T0
�
9gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMul_1MatMulfully_connected1/activationFgradients/fully_connected2/Wx_plus_b/add_grad/tuple/control_dependency*
T0*
transpose_a(*
transpose_b( 
�
Agradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/group_depsNoOp8^gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMul:^gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMul_1
�
Igradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/control_dependencyIdentity7gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMulB^gradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/group_deps*
T0*J
_class@
><loc:@gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMul
�
Kgradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/control_dependency_1Identity9gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMul_1B^gradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/group_deps*
T0*L
_classB
@>loc:@gradients/fully_connected2/Wx_plus_b/MatMul_grad/MatMul_1
�
8gradients/fully_connected1/activation_grad/LeakyReluGradLeakyReluGradIgradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/control_dependencyfully_connected1/Wx_plus_b/add*
alpha%��L>*
T0
x
3gradients/fully_connected1/Wx_plus_b/add_grad/ShapeShape!fully_connected1/Wx_plus_b/MatMul*
T0*
out_type0
c
5gradients/fully_connected1/Wx_plus_b/add_grad/Shape_1Const*
valueB: *
dtype0
�
Cgradients/fully_connected1/Wx_plus_b/add_grad/BroadcastGradientArgsBroadcastGradientArgs3gradients/fully_connected1/Wx_plus_b/add_grad/Shape5gradients/fully_connected1/Wx_plus_b/add_grad/Shape_1*
T0
�
1gradients/fully_connected1/Wx_plus_b/add_grad/SumSum8gradients/fully_connected1/activation_grad/LeakyReluGradCgradients/fully_connected1/Wx_plus_b/add_grad/BroadcastGradientArgs*

Tidx0*
	keep_dims( *
T0
�
5gradients/fully_connected1/Wx_plus_b/add_grad/ReshapeReshape1gradients/fully_connected1/Wx_plus_b/add_grad/Sum3gradients/fully_connected1/Wx_plus_b/add_grad/Shape*
T0*
Tshape0
�
3gradients/fully_connected1/Wx_plus_b/add_grad/Sum_1Sum8gradients/fully_connected1/activation_grad/LeakyReluGradEgradients/fully_connected1/Wx_plus_b/add_grad/BroadcastGradientArgs:1*
T0*

Tidx0*
	keep_dims( 
�
7gradients/fully_connected1/Wx_plus_b/add_grad/Reshape_1Reshape3gradients/fully_connected1/Wx_plus_b/add_grad/Sum_15gradients/fully_connected1/Wx_plus_b/add_grad/Shape_1*
T0*
Tshape0
�
>gradients/fully_connected1/Wx_plus_b/add_grad/tuple/group_depsNoOp6^gradients/fully_connected1/Wx_plus_b/add_grad/Reshape8^gradients/fully_connected1/Wx_plus_b/add_grad/Reshape_1
�
Fgradients/fully_connected1/Wx_plus_b/add_grad/tuple/control_dependencyIdentity5gradients/fully_connected1/Wx_plus_b/add_grad/Reshape?^gradients/fully_connected1/Wx_plus_b/add_grad/tuple/group_deps*
T0*H
_class>
<:loc:@gradients/fully_connected1/Wx_plus_b/add_grad/Reshape
�
Hgradients/fully_connected1/Wx_plus_b/add_grad/tuple/control_dependency_1Identity7gradients/fully_connected1/Wx_plus_b/add_grad/Reshape_1?^gradients/fully_connected1/Wx_plus_b/add_grad/tuple/group_deps*
T0*J
_class@
><loc:@gradients/fully_connected1/Wx_plus_b/add_grad/Reshape_1
�
7gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMulMatMulFgradients/fully_connected1/Wx_plus_b/add_grad/tuple/control_dependency&fully_connected1/weights/Variable/read*
T0*
transpose_a( *
transpose_b(
�
9gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMul_1MatMulfully_connected0/activationFgradients/fully_connected1/Wx_plus_b/add_grad/tuple/control_dependency*
T0*
transpose_a(*
transpose_b( 
�
Agradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/group_depsNoOp8^gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMul:^gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMul_1
�
Igradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/control_dependencyIdentity7gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMulB^gradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/group_deps*J
_class@
><loc:@gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMul*
T0
�
Kgradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/control_dependency_1Identity9gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMul_1B^gradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/group_deps*
T0*L
_classB
@>loc:@gradients/fully_connected1/Wx_plus_b/MatMul_grad/MatMul_1
�
8gradients/fully_connected0/activation_grad/LeakyReluGradLeakyReluGradIgradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/control_dependencyfully_connected0/Wx_plus_b/add*
T0*
alpha%��L>
x
3gradients/fully_connected0/Wx_plus_b/add_grad/ShapeShape!fully_connected0/Wx_plus_b/MatMul*
out_type0*
T0
d
5gradients/fully_connected0/Wx_plus_b/add_grad/Shape_1Const*
valueB:�*
dtype0
�
Cgradients/fully_connected0/Wx_plus_b/add_grad/BroadcastGradientArgsBroadcastGradientArgs3gradients/fully_connected0/Wx_plus_b/add_grad/Shape5gradients/fully_connected0/Wx_plus_b/add_grad/Shape_1*
T0
�
1gradients/fully_connected0/Wx_plus_b/add_grad/SumSum8gradients/fully_connected0/activation_grad/LeakyReluGradCgradients/fully_connected0/Wx_plus_b/add_grad/BroadcastGradientArgs*
T0*

Tidx0*
	keep_dims( 
�
5gradients/fully_connected0/Wx_plus_b/add_grad/ReshapeReshape1gradients/fully_connected0/Wx_plus_b/add_grad/Sum3gradients/fully_connected0/Wx_plus_b/add_grad/Shape*
T0*
Tshape0
�
3gradients/fully_connected0/Wx_plus_b/add_grad/Sum_1Sum8gradients/fully_connected0/activation_grad/LeakyReluGradEgradients/fully_connected0/Wx_plus_b/add_grad/BroadcastGradientArgs:1*

Tidx0*
	keep_dims( *
T0
�
7gradients/fully_connected0/Wx_plus_b/add_grad/Reshape_1Reshape3gradients/fully_connected0/Wx_plus_b/add_grad/Sum_15gradients/fully_connected0/Wx_plus_b/add_grad/Shape_1*
T0*
Tshape0
�
>gradients/fully_connected0/Wx_plus_b/add_grad/tuple/group_depsNoOp6^gradients/fully_connected0/Wx_plus_b/add_grad/Reshape8^gradients/fully_connected0/Wx_plus_b/add_grad/Reshape_1
�
Fgradients/fully_connected0/Wx_plus_b/add_grad/tuple/control_dependencyIdentity5gradients/fully_connected0/Wx_plus_b/add_grad/Reshape?^gradients/fully_connected0/Wx_plus_b/add_grad/tuple/group_deps*
T0*H
_class>
<:loc:@gradients/fully_connected0/Wx_plus_b/add_grad/Reshape
�
Hgradients/fully_connected0/Wx_plus_b/add_grad/tuple/control_dependency_1Identity7gradients/fully_connected0/Wx_plus_b/add_grad/Reshape_1?^gradients/fully_connected0/Wx_plus_b/add_grad/tuple/group_deps*
T0*J
_class@
><loc:@gradients/fully_connected0/Wx_plus_b/add_grad/Reshape_1
�
7gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMulMatMulFgradients/fully_connected0/Wx_plus_b/add_grad/tuple/control_dependency&fully_connected0/weights/Variable/read*
transpose_a( *
transpose_b(*
T0
�
9gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMul_1MatMulinputFgradients/fully_connected0/Wx_plus_b/add_grad/tuple/control_dependency*
T0*
transpose_a(*
transpose_b( 
�
Agradients/fully_connected0/Wx_plus_b/MatMul_grad/tuple/group_depsNoOp8^gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMul:^gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMul_1
�
Igradients/fully_connected0/Wx_plus_b/MatMul_grad/tuple/control_dependencyIdentity7gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMulB^gradients/fully_connected0/Wx_plus_b/MatMul_grad/tuple/group_deps*
T0*J
_class@
><loc:@gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMul
�
Kgradients/fully_connected0/Wx_plus_b/MatMul_grad/tuple/control_dependency_1Identity9gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMul_1B^gradients/fully_connected0/Wx_plus_b/MatMul_grad/tuple/group_deps*
T0*L
_classB
@>loc:@gradients/fully_connected0/Wx_plus_b/MatMul_grad/MatMul_1
{
beta1_power/initial_valueConst*3
_class)
'%loc:@fully_connected0/biases/Variable*
valueB
 *fff?*
dtype0
�
beta1_power
VariableV2*
shared_name *3
_class)
'%loc:@fully_connected0/biases/Variable*
dtype0*
	container *
shape: 
�
beta1_power/AssignAssignbeta1_powerbeta1_power/initial_value*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
g
beta1_power/readIdentitybeta1_power*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
{
beta2_power/initial_valueConst*3
_class)
'%loc:@fully_connected0/biases/Variable*
valueB
 *w�?*
dtype0
�
beta2_power
VariableV2*
shared_name *3
_class)
'%loc:@fully_connected0/biases/Variable*
dtype0*
	container *
shape: 
�
beta2_power/AssignAssignbeta2_powerbeta2_power/initial_value*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
g
beta2_power/readIdentitybeta2_power*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
Mfully_connected0/weights/Variable/optimizer/Initializer/zeros/shape_as_tensorConst*
valueB"I   �   *4
_class*
(&loc:@fully_connected0/weights/Variable*
dtype0
�
Cfully_connected0/weights/Variable/optimizer/Initializer/zeros/ConstConst*
valueB
 *    *4
_class*
(&loc:@fully_connected0/weights/Variable*
dtype0
�
=fully_connected0/weights/Variable/optimizer/Initializer/zerosFillMfully_connected0/weights/Variable/optimizer/Initializer/zeros/shape_as_tensorCfully_connected0/weights/Variable/optimizer/Initializer/zeros/Const*
T0*

index_type0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
+fully_connected0/weights/Variable/optimizer
VariableV2*
	container *
shape:	I�*
shared_name *4
_class*
(&loc:@fully_connected0/weights/Variable*
dtype0
�
2fully_connected0/weights/Variable/optimizer/AssignAssign+fully_connected0/weights/Variable/optimizer=fully_connected0/weights/Variable/optimizer/Initializer/zeros*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
0fully_connected0/weights/Variable/optimizer/readIdentity+fully_connected0/weights/Variable/optimizer*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
Ofully_connected0/weights/Variable/optimizer_1/Initializer/zeros/shape_as_tensorConst*
valueB"I   �   *4
_class*
(&loc:@fully_connected0/weights/Variable*
dtype0
�
Efully_connected0/weights/Variable/optimizer_1/Initializer/zeros/ConstConst*
valueB
 *    *4
_class*
(&loc:@fully_connected0/weights/Variable*
dtype0
�
?fully_connected0/weights/Variable/optimizer_1/Initializer/zerosFillOfully_connected0/weights/Variable/optimizer_1/Initializer/zeros/shape_as_tensorEfully_connected0/weights/Variable/optimizer_1/Initializer/zeros/Const*
T0*

index_type0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
-fully_connected0/weights/Variable/optimizer_1
VariableV2*
dtype0*
	container *
shape:	I�*
shared_name *4
_class*
(&loc:@fully_connected0/weights/Variable
�
4fully_connected0/weights/Variable/optimizer_1/AssignAssign-fully_connected0/weights/Variable/optimizer_1?fully_connected0/weights/Variable/optimizer_1/Initializer/zeros*
use_locking(*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable*
validate_shape(
�
2fully_connected0/weights/Variable/optimizer_1/readIdentity-fully_connected0/weights/Variable/optimizer_1*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
<fully_connected0/biases/Variable/optimizer/Initializer/zerosConst*
valueB�*    *3
_class)
'%loc:@fully_connected0/biases/Variable*
dtype0
�
*fully_connected0/biases/Variable/optimizer
VariableV2*
shared_name *3
_class)
'%loc:@fully_connected0/biases/Variable*
dtype0*
	container *
shape:�
�
1fully_connected0/biases/Variable/optimizer/AssignAssign*fully_connected0/biases/Variable/optimizer<fully_connected0/biases/Variable/optimizer/Initializer/zeros*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
�
/fully_connected0/biases/Variable/optimizer/readIdentity*fully_connected0/biases/Variable/optimizer*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
>fully_connected0/biases/Variable/optimizer_1/Initializer/zerosConst*
valueB�*    *3
_class)
'%loc:@fully_connected0/biases/Variable*
dtype0
�
,fully_connected0/biases/Variable/optimizer_1
VariableV2*3
_class)
'%loc:@fully_connected0/biases/Variable*
dtype0*
	container *
shape:�*
shared_name 
�
3fully_connected0/biases/Variable/optimizer_1/AssignAssign,fully_connected0/biases/Variable/optimizer_1>fully_connected0/biases/Variable/optimizer_1/Initializer/zeros*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
�
1fully_connected0/biases/Variable/optimizer_1/readIdentity,fully_connected0/biases/Variable/optimizer_1*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
Mfully_connected1/weights/Variable/optimizer/Initializer/zeros/shape_as_tensorConst*
dtype0*
valueB"�       *4
_class*
(&loc:@fully_connected1/weights/Variable
�
Cfully_connected1/weights/Variable/optimizer/Initializer/zeros/ConstConst*
valueB
 *    *4
_class*
(&loc:@fully_connected1/weights/Variable*
dtype0
�
=fully_connected1/weights/Variable/optimizer/Initializer/zerosFillMfully_connected1/weights/Variable/optimizer/Initializer/zeros/shape_as_tensorCfully_connected1/weights/Variable/optimizer/Initializer/zeros/Const*
T0*

index_type0*4
_class*
(&loc:@fully_connected1/weights/Variable
�
+fully_connected1/weights/Variable/optimizer
VariableV2*
shared_name *4
_class*
(&loc:@fully_connected1/weights/Variable*
dtype0*
	container *
shape:	� 
�
2fully_connected1/weights/Variable/optimizer/AssignAssign+fully_connected1/weights/Variable/optimizer=fully_connected1/weights/Variable/optimizer/Initializer/zeros*
use_locking(*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable*
validate_shape(
�
0fully_connected1/weights/Variable/optimizer/readIdentity+fully_connected1/weights/Variable/optimizer*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable
�
Ofully_connected1/weights/Variable/optimizer_1/Initializer/zeros/shape_as_tensorConst*
valueB"�       *4
_class*
(&loc:@fully_connected1/weights/Variable*
dtype0
�
Efully_connected1/weights/Variable/optimizer_1/Initializer/zeros/ConstConst*
valueB
 *    *4
_class*
(&loc:@fully_connected1/weights/Variable*
dtype0
�
?fully_connected1/weights/Variable/optimizer_1/Initializer/zerosFillOfully_connected1/weights/Variable/optimizer_1/Initializer/zeros/shape_as_tensorEfully_connected1/weights/Variable/optimizer_1/Initializer/zeros/Const*
T0*

index_type0*4
_class*
(&loc:@fully_connected1/weights/Variable
�
-fully_connected1/weights/Variable/optimizer_1
VariableV2*4
_class*
(&loc:@fully_connected1/weights/Variable*
dtype0*
	container *
shape:	� *
shared_name 
�
4fully_connected1/weights/Variable/optimizer_1/AssignAssign-fully_connected1/weights/Variable/optimizer_1?fully_connected1/weights/Variable/optimizer_1/Initializer/zeros*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable*
validate_shape(*
use_locking(
�
2fully_connected1/weights/Variable/optimizer_1/readIdentity-fully_connected1/weights/Variable/optimizer_1*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable
�
<fully_connected1/biases/Variable/optimizer/Initializer/zerosConst*
dtype0*
valueB *    *3
_class)
'%loc:@fully_connected1/biases/Variable
�
*fully_connected1/biases/Variable/optimizer
VariableV2*
dtype0*
	container *
shape: *
shared_name *3
_class)
'%loc:@fully_connected1/biases/Variable
�
1fully_connected1/biases/Variable/optimizer/AssignAssign*fully_connected1/biases/Variable/optimizer<fully_connected1/biases/Variable/optimizer/Initializer/zeros*
use_locking(*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable*
validate_shape(
�
/fully_connected1/biases/Variable/optimizer/readIdentity*fully_connected1/biases/Variable/optimizer*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable
�
>fully_connected1/biases/Variable/optimizer_1/Initializer/zerosConst*
valueB *    *3
_class)
'%loc:@fully_connected1/biases/Variable*
dtype0
�
,fully_connected1/biases/Variable/optimizer_1
VariableV2*
shape: *
shared_name *3
_class)
'%loc:@fully_connected1/biases/Variable*
dtype0*
	container 
�
3fully_connected1/biases/Variable/optimizer_1/AssignAssign,fully_connected1/biases/Variable/optimizer_1>fully_connected1/biases/Variable/optimizer_1/Initializer/zeros*
use_locking(*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable*
validate_shape(
�
1fully_connected1/biases/Variable/optimizer_1/readIdentity,fully_connected1/biases/Variable/optimizer_1*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable
�
=fully_connected2/weights/Variable/optimizer/Initializer/zerosConst*
valueB *    *4
_class*
(&loc:@fully_connected2/weights/Variable*
dtype0
�
+fully_connected2/weights/Variable/optimizer
VariableV2*4
_class*
(&loc:@fully_connected2/weights/Variable*
dtype0*
	container *
shape
: *
shared_name 
�
2fully_connected2/weights/Variable/optimizer/AssignAssign+fully_connected2/weights/Variable/optimizer=fully_connected2/weights/Variable/optimizer/Initializer/zeros*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable*
validate_shape(*
use_locking(
�
0fully_connected2/weights/Variable/optimizer/readIdentity+fully_connected2/weights/Variable/optimizer*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable
�
?fully_connected2/weights/Variable/optimizer_1/Initializer/zerosConst*
valueB *    *4
_class*
(&loc:@fully_connected2/weights/Variable*
dtype0
�
-fully_connected2/weights/Variable/optimizer_1
VariableV2*
shape
: *
shared_name *4
_class*
(&loc:@fully_connected2/weights/Variable*
dtype0*
	container 
�
4fully_connected2/weights/Variable/optimizer_1/AssignAssign-fully_connected2/weights/Variable/optimizer_1?fully_connected2/weights/Variable/optimizer_1/Initializer/zeros*
use_locking(*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable*
validate_shape(
�
2fully_connected2/weights/Variable/optimizer_1/readIdentity-fully_connected2/weights/Variable/optimizer_1*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable
�
<fully_connected2/biases/Variable/optimizer/Initializer/zerosConst*
valueB*    *3
_class)
'%loc:@fully_connected2/biases/Variable*
dtype0
�
*fully_connected2/biases/Variable/optimizer
VariableV2*
shared_name *3
_class)
'%loc:@fully_connected2/biases/Variable*
dtype0*
	container *
shape:
�
1fully_connected2/biases/Variable/optimizer/AssignAssign*fully_connected2/biases/Variable/optimizer<fully_connected2/biases/Variable/optimizer/Initializer/zeros*
use_locking(*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable*
validate_shape(
�
/fully_connected2/biases/Variable/optimizer/readIdentity*fully_connected2/biases/Variable/optimizer*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable
�
>fully_connected2/biases/Variable/optimizer_1/Initializer/zerosConst*
valueB*    *3
_class)
'%loc:@fully_connected2/biases/Variable*
dtype0
�
,fully_connected2/biases/Variable/optimizer_1
VariableV2*
shared_name *3
_class)
'%loc:@fully_connected2/biases/Variable*
dtype0*
	container *
shape:
�
3fully_connected2/biases/Variable/optimizer_1/AssignAssign,fully_connected2/biases/Variable/optimizer_1>fully_connected2/biases/Variable/optimizer_1/Initializer/zeros*
use_locking(*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable*
validate_shape(
�
1fully_connected2/biases/Variable/optimizer_1/readIdentity,fully_connected2/biases/Variable/optimizer_1*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable
@
train/learning_rateConst*
valueB
 *o�:*
dtype0
8
train/beta1Const*
valueB
 *fff?*
dtype0
8
train/beta2Const*
valueB
 *w�?*
dtype0
:
train/epsilonConst*
valueB
 *w�+2*
dtype0
�
8train/update_fully_connected0/weights/Variable/ApplyAdam	ApplyAdam!fully_connected0/weights/Variable+fully_connected0/weights/Variable/optimizer-fully_connected0/weights/Variable/optimizer_1beta1_power/readbeta2_power/readtrain/learning_ratetrain/beta1train/beta2train/epsilonKgradients/fully_connected0/Wx_plus_b/MatMul_grad/tuple/control_dependency_1*
use_nesterov( *
use_locking( *
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
7train/update_fully_connected0/biases/Variable/ApplyAdam	ApplyAdam fully_connected0/biases/Variable*fully_connected0/biases/Variable/optimizer,fully_connected0/biases/Variable/optimizer_1beta1_power/readbeta2_power/readtrain/learning_ratetrain/beta1train/beta2train/epsilonHgradients/fully_connected0/Wx_plus_b/add_grad/tuple/control_dependency_1*
use_locking( *
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
use_nesterov( 
�
8train/update_fully_connected1/weights/Variable/ApplyAdam	ApplyAdam!fully_connected1/weights/Variable+fully_connected1/weights/Variable/optimizer-fully_connected1/weights/Variable/optimizer_1beta1_power/readbeta2_power/readtrain/learning_ratetrain/beta1train/beta2train/epsilonKgradients/fully_connected1/Wx_plus_b/MatMul_grad/tuple/control_dependency_1*
use_locking( *
T0*4
_class*
(&loc:@fully_connected1/weights/Variable*
use_nesterov( 
�
7train/update_fully_connected1/biases/Variable/ApplyAdam	ApplyAdam fully_connected1/biases/Variable*fully_connected1/biases/Variable/optimizer,fully_connected1/biases/Variable/optimizer_1beta1_power/readbeta2_power/readtrain/learning_ratetrain/beta1train/beta2train/epsilonHgradients/fully_connected1/Wx_plus_b/add_grad/tuple/control_dependency_1*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable*
use_nesterov( *
use_locking( 
�
8train/update_fully_connected2/weights/Variable/ApplyAdam	ApplyAdam!fully_connected2/weights/Variable+fully_connected2/weights/Variable/optimizer-fully_connected2/weights/Variable/optimizer_1beta1_power/readbeta2_power/readtrain/learning_ratetrain/beta1train/beta2train/epsilonKgradients/fully_connected2/Wx_plus_b/MatMul_grad/tuple/control_dependency_1*4
_class*
(&loc:@fully_connected2/weights/Variable*
use_nesterov( *
use_locking( *
T0
�
7train/update_fully_connected2/biases/Variable/ApplyAdam	ApplyAdam fully_connected2/biases/Variable*fully_connected2/biases/Variable/optimizer,fully_connected2/biases/Variable/optimizer_1beta1_power/readbeta2_power/readtrain/learning_ratetrain/beta1train/beta2train/epsilonHgradients/fully_connected2/Wx_plus_b/add_grad/tuple/control_dependency_1*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable*
use_nesterov( *
use_locking( 
�
	train/mulMulbeta1_power/readtrain/beta18^train/update_fully_connected0/biases/Variable/ApplyAdam9^train/update_fully_connected0/weights/Variable/ApplyAdam8^train/update_fully_connected1/biases/Variable/ApplyAdam9^train/update_fully_connected1/weights/Variable/ApplyAdam8^train/update_fully_connected2/biases/Variable/ApplyAdam9^train/update_fully_connected2/weights/Variable/ApplyAdam*3
_class)
'%loc:@fully_connected0/biases/Variable*
T0
�
train/AssignAssignbeta1_power	train/mul*
validate_shape(*
use_locking( *
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
train/mul_1Mulbeta2_power/readtrain/beta28^train/update_fully_connected0/biases/Variable/ApplyAdam9^train/update_fully_connected0/weights/Variable/ApplyAdam8^train/update_fully_connected1/biases/Variable/ApplyAdam9^train/update_fully_connected1/weights/Variable/ApplyAdam8^train/update_fully_connected2/biases/Variable/ApplyAdam9^train/update_fully_connected2/weights/Variable/ApplyAdam*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable
�
train/Assign_1Assignbeta2_powertrain/mul_1*
use_locking( *
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
�
trainNoOp^train/Assign^train/Assign_18^train/update_fully_connected0/biases/Variable/ApplyAdam9^train/update_fully_connected0/weights/Variable/ApplyAdam8^train/update_fully_connected1/biases/Variable/ApplyAdam9^train/update_fully_connected1/weights/Variable/ApplyAdam8^train/update_fully_connected2/biases/Variable/ApplyAdam9^train/update_fully_connected2/weights/Variable/ApplyAdam
�
initNoOp^beta1_power/Assign^beta2_power/Assign(^fully_connected0/biases/Variable/Assign2^fully_connected0/biases/Variable/optimizer/Assign4^fully_connected0/biases/Variable/optimizer_1/Assign)^fully_connected0/weights/Variable/Assign3^fully_connected0/weights/Variable/optimizer/Assign5^fully_connected0/weights/Variable/optimizer_1/Assign(^fully_connected1/biases/Variable/Assign2^fully_connected1/biases/Variable/optimizer/Assign4^fully_connected1/biases/Variable/optimizer_1/Assign)^fully_connected1/weights/Variable/Assign3^fully_connected1/weights/Variable/optimizer/Assign5^fully_connected1/weights/Variable/optimizer_1/Assign(^fully_connected2/biases/Variable/Assign2^fully_connected2/biases/Variable/optimizer/Assign4^fully_connected2/biases/Variable/optimizer_1/Assign)^fully_connected2/weights/Variable/Assign3^fully_connected2/weights/Variable/optimizer/Assign5^fully_connected2/weights/Variable/optimizer_1/Assign
A
save/filename/inputConst*
valueB Bmodel*
dtype0
V
save/filenamePlaceholderWithDefaultsave/filename/input*
dtype0*
shape: 
M

save/ConstPlaceholderWithDefaultsave/filename*
shape: *
dtype0
�
save/SaveV2/tensor_namesConst*�
value�B�Bbeta1_powerBbeta2_powerB fully_connected0/biases/VariableB*fully_connected0/biases/Variable/optimizerB,fully_connected0/biases/Variable/optimizer_1B!fully_connected0/weights/VariableB+fully_connected0/weights/Variable/optimizerB-fully_connected0/weights/Variable/optimizer_1B fully_connected1/biases/VariableB*fully_connected1/biases/Variable/optimizerB,fully_connected1/biases/Variable/optimizer_1B!fully_connected1/weights/VariableB+fully_connected1/weights/Variable/optimizerB-fully_connected1/weights/Variable/optimizer_1B fully_connected2/biases/VariableB*fully_connected2/biases/Variable/optimizerB,fully_connected2/biases/Variable/optimizer_1B!fully_connected2/weights/VariableB+fully_connected2/weights/Variable/optimizerB-fully_connected2/weights/Variable/optimizer_1*
dtype0
o
save/SaveV2/shape_and_slicesConst*;
value2B0B B B B B B B B B B B B B B B B B B B B *
dtype0
�
save/SaveV2SaveV2
save/Constsave/SaveV2/tensor_namessave/SaveV2/shape_and_slicesbeta1_powerbeta2_power fully_connected0/biases/Variable*fully_connected0/biases/Variable/optimizer,fully_connected0/biases/Variable/optimizer_1!fully_connected0/weights/Variable+fully_connected0/weights/Variable/optimizer-fully_connected0/weights/Variable/optimizer_1 fully_connected1/biases/Variable*fully_connected1/biases/Variable/optimizer,fully_connected1/biases/Variable/optimizer_1!fully_connected1/weights/Variable+fully_connected1/weights/Variable/optimizer-fully_connected1/weights/Variable/optimizer_1 fully_connected2/biases/Variable*fully_connected2/biases/Variable/optimizer,fully_connected2/biases/Variable/optimizer_1!fully_connected2/weights/Variable+fully_connected2/weights/Variable/optimizer-fully_connected2/weights/Variable/optimizer_1*"
dtypes
2
e
save/control_dependencyIdentity
save/Const^save/SaveV2*
T0*
_class
loc:@save/Const
�
save/RestoreV2/tensor_namesConst"/device:CPU:0*�
value�B�Bbeta1_powerBbeta2_powerB fully_connected0/biases/VariableB*fully_connected0/biases/Variable/optimizerB,fully_connected0/biases/Variable/optimizer_1B!fully_connected0/weights/VariableB+fully_connected0/weights/Variable/optimizerB-fully_connected0/weights/Variable/optimizer_1B fully_connected1/biases/VariableB*fully_connected1/biases/Variable/optimizerB,fully_connected1/biases/Variable/optimizer_1B!fully_connected1/weights/VariableB+fully_connected1/weights/Variable/optimizerB-fully_connected1/weights/Variable/optimizer_1B fully_connected2/biases/VariableB*fully_connected2/biases/Variable/optimizerB,fully_connected2/biases/Variable/optimizer_1B!fully_connected2/weights/VariableB+fully_connected2/weights/Variable/optimizerB-fully_connected2/weights/Variable/optimizer_1*
dtype0
�
save/RestoreV2/shape_and_slicesConst"/device:CPU:0*;
value2B0B B B B B B B B B B B B B B B B B B B B *
dtype0
�
save/RestoreV2	RestoreV2
save/Constsave/RestoreV2/tensor_namessave/RestoreV2/shape_and_slices"/device:CPU:0*"
dtypes
2
�
save/AssignAssignbeta1_powersave/RestoreV2*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(*
use_locking(*
T0
�
save/Assign_1Assignbeta2_powersave/RestoreV2:1*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
�
save/Assign_2Assign fully_connected0/biases/Variablesave/RestoreV2:2*
use_locking(*
T0*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(
�
save/Assign_3Assign*fully_connected0/biases/Variable/optimizersave/RestoreV2:3*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(*
use_locking(*
T0
�
save/Assign_4Assign,fully_connected0/biases/Variable/optimizer_1save/RestoreV2:4*3
_class)
'%loc:@fully_connected0/biases/Variable*
validate_shape(*
use_locking(*
T0
�
save/Assign_5Assign!fully_connected0/weights/Variablesave/RestoreV2:5*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
save/Assign_6Assign+fully_connected0/weights/Variable/optimizersave/RestoreV2:6*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable
�
save/Assign_7Assign-fully_connected0/weights/Variable/optimizer_1save/RestoreV2:7*
use_locking(*
T0*4
_class*
(&loc:@fully_connected0/weights/Variable*
validate_shape(
�
save/Assign_8Assign fully_connected1/biases/Variablesave/RestoreV2:8*
use_locking(*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable*
validate_shape(
�
save/Assign_9Assign*fully_connected1/biases/Variable/optimizersave/RestoreV2:9*
validate_shape(*
use_locking(*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable
�
save/Assign_10Assign,fully_connected1/biases/Variable/optimizer_1save/RestoreV2:10*
validate_shape(*
use_locking(*
T0*3
_class)
'%loc:@fully_connected1/biases/Variable
�
save/Assign_11Assign!fully_connected1/weights/Variablesave/RestoreV2:11*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable
�
save/Assign_12Assign+fully_connected1/weights/Variable/optimizersave/RestoreV2:12*
use_locking(*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable*
validate_shape(
�
save/Assign_13Assign-fully_connected1/weights/Variable/optimizer_1save/RestoreV2:13*
use_locking(*
T0*4
_class*
(&loc:@fully_connected1/weights/Variable*
validate_shape(
�
save/Assign_14Assign fully_connected2/biases/Variablesave/RestoreV2:14*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable*
validate_shape(*
use_locking(
�
save/Assign_15Assign*fully_connected2/biases/Variable/optimizersave/RestoreV2:15*
validate_shape(*
use_locking(*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable
�
save/Assign_16Assign,fully_connected2/biases/Variable/optimizer_1save/RestoreV2:16*
T0*3
_class)
'%loc:@fully_connected2/biases/Variable*
validate_shape(*
use_locking(
�
save/Assign_17Assign!fully_connected2/weights/Variablesave/RestoreV2:17*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable
�
save/Assign_18Assign+fully_connected2/weights/Variable/optimizersave/RestoreV2:18*
use_locking(*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable*
validate_shape(
�
save/Assign_19Assign-fully_connected2/weights/Variable/optimizer_1save/RestoreV2:19*
validate_shape(*
use_locking(*
T0*4
_class*
(&loc:@fully_connected2/weights/Variable
�
save/restore_allNoOp^save/Assign^save/Assign_1^save/Assign_10^save/Assign_11^save/Assign_12^save/Assign_13^save/Assign_14^save/Assign_15^save/Assign_16^save/Assign_17^save/Assign_18^save/Assign_19^save/Assign_2^save/Assign_3^save/Assign_4^save/Assign_5^save/Assign_6^save/Assign_7^save/Assign_8^save/Assign_9"